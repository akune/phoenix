package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.List;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.crypto.KeyStore;
import de.kune.phoenix.shared.Message;

public class PollingRestMessageReceiver {

	private static final int pollInterval = 5000;

	public static interface DecryptedMessageHandler {
		void handleMessage(Message message, byte[] content);
	}

	private final Timer pollingRestReceiverTimer = new Timer() {

		private String lastTransmission;

		@Override
		public void run() {
			MethodCallback<List<Message>> messageHandler = new MethodCallback<List<Message>>() {

				@Override
				public void onSuccess(Method method, List<Message> response) {
					for (Message message : keyMessagesFirst(response)) {
						if (lastTransmission == null
								|| lastTransmission.compareTo(message.getTransmission()) < 0) {
							lastTransmission = message.getTransmission();
						}
						try {
							GWT.log("received message from " + (conversationId == null ? "general" : conversationId)
									+ ": " + message);
							validateSignature(message);
							processMessage(message);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					pollingRestReceiverTimer.schedule(pollInterval);
				}

				private List<Message> keyMessagesFirst(List<Message> response) {
					List<Message> keyMessages = new ArrayList<Message>();
					List<Message> otherMessages = new ArrayList<Message>();
					for (Message m : response) {
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
						handler.handleMessage(message, message.getContent());
					} else if (keyStore.getDecryptionKey(message.getKeyId()) != null) {
						handler.handleMessage(message,
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
					pollingRestReceiverTimer.schedule(5000);
					throw new RuntimeException(exception);
				}
			};
			messageService.get(false, lastTransmission, receiverId, conversationId, messageHandler);
		}
	};

	private final MessageService messageService = MessageService.instance();

	private KeyStore keyStore;

	private String receiverId;

	private String conversationId;

	private DecryptedMessageHandler handler;

	public PollingRestMessageReceiver(String receiverId, KeyStore keyStore, DecryptedMessageHandler handler) {
		this.receiverId = receiverId;
		this.keyStore = keyStore;
		this.handler = handler;
	}

	public PollingRestMessageReceiver(String receiverId, String conversationId, KeyStore keyStore, DecryptedMessageHandler handler) {
		this(receiverId, keyStore, handler);
		this.conversationId = conversationId;
	}

	public void start() {
		pollingRestReceiverTimer.schedule(pollInterval);
	}

}
