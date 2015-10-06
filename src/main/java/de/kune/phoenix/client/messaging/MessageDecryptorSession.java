package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.GWT;

import de.kune.phoenix.client.crypto.KeyStore;
import de.kune.phoenix.client.messaging.PollingRestMessageReceiver.DecryptedMessageHandler;
import de.kune.phoenix.shared.Message;

public class MessageDecryptorSession implements Processor<Message> {

	private KeyStore keyStore;
	private DecryptedMessageHandler handler;
	private Message lastProcessedObject;

	public MessageDecryptorSession(KeyStore keyStore, DecryptedMessageHandler handler) {
		this.keyStore = keyStore;
		this.handler = handler;
	}

	@Override
	public Message getLastProcessedObject() {
		return lastProcessedObject;
	}

	public void process(Collection<Message> response) {
		for (Message message : keyMessagesFirst(response)) {
			try {
				GWT.log("processing message from "
						+ (message.getConversationId() == null ? "general" : message.getConversationId()) + ": "
						+ message);
				validateSignature(message);
				processMessage(message);
				lastProcessedObject = message;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private List<Message> keyMessagesFirst(Collection<Message> response) {
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
			handler.handleMessage(message, message.getDecryptedContent(keyStore.getDecryptionKey(message.getKeyId())));
		} else {
			throw new IllegalStateException("could not decrypt message, unknown key <" + message.getKeyId() + ">");
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

}
