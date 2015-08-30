package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.List;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.crypto.KeyStore;
import de.kune.phoenix.shared.Message;
import de.kune.phoenix.shared.MessageService;

public abstract class AbstractMessageReceiver {

	private final Timer messageReceiverTimer = new Timer() {

		private Long lastTransmission;

		@Override
		public void run() {
			GWT.log("running client session message timer");
			MethodCallback<List<Message>> messageHandler = new MethodCallback<List<Message>>() {

				@Override
				public void onSuccess(Method method, List<Message> response) {
					for (Message message : keyMessagesFirst(response)) {
						if (lastTransmission == null
								|| lastTransmission.compareTo(message.getTransmission().getTime()) < 0) {
							lastTransmission = message.getTransmission().getTime();
						}
						try {
							GWT.log("received message from " + (conversationId == null ? " general" : conversationId)
									+ ": " + message);
							validateSignature(message);
							processMessage(message);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					messageReceiverTimer.schedule(100);
				}

				private List<Message> keyMessagesFirst(List<Message> response) {
					List<Message> keyMessages = new ArrayList<Message>();
					List<Message> otherMessages = new ArrayList<Message>();
					for (Message m: response) {
						if (m.getMessageType() == Message.Type.SECRET_KEY) {
							keyMessages.add(m);
						} else {
							otherMessages.add(m);
						}
					}
					List<Message> result = new ArrayList<Message>();
					result.addAll(keyMessages);
					result.addAll(otherMessages);
					return result;
				}

				private void processMessage(Message message) {
					if (message.getKeyId() == null) {
						handleReceivedMessage(message, message.getContent());
					} else if (keyStore.getDecryptionKey(message.getKeyId()) != null) {
						handleReceivedMessage(message,
								message.getDecryptedContent(keyStore.getDecryptionKey(message.getKeyId())));
					} else {
						throw new IllegalStateException(
								"could not decrypt message, unknown key <" + message.getKeyId() + ">");
					}
				}

				private void validateSignature(Message message) {
					if (message.getSenderId() != null && message.getSignature() != null
							&& keyStore.getPublicKey(message.getSenderId()) != null) {
						if (!message.checkSignature(keyStore.getPublicKey(message.getSenderId()))) {
							throw new IllegalStateException("verifying message failed, invalid signature");
						}
					} else {
						if (message.getSignature() == null) {
							GWT.log("message not verified, no signature");
						} else if (message.getSenderId() == null) {
							GWT.log("message not verified, no sender id");
						} else if (keyStore.getPublicKey(message.getSenderId()) == null) {
							GWT.log("message not verified, unknown sender id");
						}
					}
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					messageReceiverTimer.schedule(5000);
					throw new RuntimeException(exception);
				}
			};
			if (conversationId == null) {
				messageService.get(true, lastTransmission, receiverId, messageHandler);
			} else {
				messageService.getFromConversation(conversationId, true, lastTransmission, receiverId, messageHandler);
			}
		}
	};

	private final MessageService messageService = GWT.create(MessageService.class);

	private final String receiverId;

	private final KeyStore keyStore;

	private String conversationId;

	public AbstractMessageReceiver(String receiverId, String conversationId, KeyStore keyStore) {
		this.receiverId = receiverId;
		this.conversationId = conversationId;
		this.keyStore = keyStore;
	}

	protected abstract void handleReceivedMessage(final Message message, byte[] decryptedContent);

	public void start() {
		messageReceiverTimer.schedule(100);
	}

}
