package de.kune.phoenix.client.messaging;

import static de.kune.phoenix.client.functional.Predicate.containsSender;
import static de.kune.phoenix.client.functional.Predicate.hasConversationId;
import static de.kune.phoenix.client.functional.Predicate.hasType;
import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.crypto.SecretKey.KeyStrength;
import de.kune.phoenix.client.crypto.SymmetricCipher;
import de.kune.phoenix.client.functional.MessageHandler;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.shared.Message;

public class Conversation {

	private static final int KEY_LIFESPAN_MILLIS = 10 * 1000;

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private KeyPair keyPair;
		private String conversationId;
		private String[] recipientIds;
		private Map<String, PublicKey> sharedPublicKeys;
		private MessageHandler receivedMessageHandler;

		public Builder keyPair(KeyPair keyPair) {
			this.keyPair = keyPair;
			return this;
		}

		public Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		public Builder recipientIds(String... recipientIds) {
			this.recipientIds = Arrays.copyOf(recipientIds, recipientIds.length);
			return this;
		}

		public Builder sharedPublicKeys(Map<String, PublicKey> sharedPublicKeys) {
			this.sharedPublicKeys = sharedPublicKeys;
			return this;
		}

		public Builder receivedMessageHandler(MessageHandler receivedMessageHandler) {
			this.receivedMessageHandler = receivedMessageHandler;
			return this;
		}

		public Conversation build() {
			if (keyPair == null || conversationId == null || recipientIds == null || recipientIds.length == 0
					|| sharedPublicKeys == null || receivedMessageHandler == null) {
				throw new IllegalStateException("unset field(s)");
			}
			return new Conversation(keyPair, sharedPublicKeys, conversationId, recipientIds, receivedMessageHandler);
		}

		public String getConversationId() {
			return conversationId;
		}
	}

	private final KeyPair keyPair;
	private final MessageService messageService = MessageService.instance();
	private final String conversationId;
	private final Map<String, PublicKey> sharedPublicKeys;
	private final Map<String, SecretKey> secretKeys = new HashMap<>();
	private final Map<String, SecretKey> deprecatedSecretKeys = new HashMap<>();
	private Set<String> participants = new HashSet<>();
	private final MessageHandler receivedMessageHandler;

	private Conversation(KeyPair keyPair, Map<String, PublicKey> sharedPublicKeys, String conversationId,
			String[] recipientIds, MessageHandler receivedMessageHandler) {
		this.keyPair = keyPair;
		this.sharedPublicKeys = sharedPublicKeys;
		this.conversationId = conversationId;
		this.receivedMessageHandler = receivedMessageHandler;
		participants.addAll(Arrays.asList(recipientIds));
		messageService.addMessageHandler(isSecretKey(), this::handleSecretKey);
		messageService.addMessageHandler(isIntroduction(), this::handleIntroduction);
		messageService.addMessageHandler(isTextMessage(), this::handleTextMessage);
	}

	public String getConversationId() {
		return conversationId;
	}

	private Predicate<Message> isTextMessage() {
		return hasConversationId(conversationId).and(hasType(Message.Type.PLAIN_TEXT));
	}

	private Predicate<Message> isSecretKey() {
		return hasConversationId(conversationId).and(hasType(Message.Type.SECRET_KEY));
	}

	private Predicate<Message> isIntroduction() {
		return containsSender(participants).and(hasConversationId(conversationId))
				.and(hasType(Message.Type.INTRODUCTION));
	}

	private void handleSecretKey(Message message, byte[] data) {
		GWT.log("received secret key message: " + message);
		decrypt(message, (m, c) -> addSecretKey(SymmetricCipher.Factory.createSecretKey(c)));
	}

	private void addSecretKey(SecretKey key) {
		secretKeys.put(key.getId(), key);
		new Timer() {
			@Override
			public void run() {
				deprecateSecretKey(key);
			}

		}.schedule(KEY_LIFESPAN_MILLIS);
	}

	private void deprecateAllSecretKeys() {
		while (!secretKeys.isEmpty()) {
			SecretKey key = secretKeys.values().iterator().next();
			deprecateSecretKey(key);
		}
	}

	private void deprecateSecretKey(SecretKey key) {
		GWT.log("deprecating secret key [" + key.getId() + "]");
		deprecatedSecretKeys.put(key.getId(), key);
		secretKeys.remove(key.getId());
	}

	private void handleIntroduction(Message message, byte[] data) {
		GWT.log("received introduction of new participant to conversation [" + conversationId + "]");
		PublicKey extractedPublicKey = AsymmetricCipher.Factory.createPublicKey(message.getContent());
		sharedPublicKeys.put(extractedPublicKey.getId(), extractedPublicKey);
		participants.add(extractedPublicKey.getId());
		deprecateAllSecretKeys();
	}

	private void handleTextMessage(Message message, byte[] data) {
		GWT.log("received text message: " + message);
		decrypt(message, (m, c) -> receivedMessageHandler.handleReceivedMessage(m, c));
	}

	private void decrypt(Message message, MessageHandler messageHandler) {
		if (message.getKeyId() == null) {
			throw new IllegalStateException("tried to decrypt unencrypted message");
		}
		Key encryptionKey = getEncryptionKey(message.getKeyId());
		if (encryptionKey == null) {
			throw new IllegalStateException("unknown encryption key [" + message.getKeyId() + "]");
		}
		messageHandler.handleReceivedMessage(message, message.getDecryptedContent(encryptionKey));
	}

	/**
	 * Introduces a new participant to the conversation. Sends the invitor's
	 * self-signed public key to the new participant and an introduction message
	 * (containing the new participant's public key) to all participants of this
	 * conversation (including the new participant).
	 * 
	 * @param participant
	 *            the new participant's public key
	 */
	public void introduce(PublicKey participant) {
		participants.add(participant.getId());
		GWT.log("introducing " + participant + " sending introduction to " + participants);
		messageService.send(asList(selfSignedPublicKey(participant), introductionMessage(participant)));
	}

	private Message selfSignedPublicKey(PublicKey participant) {
		Message message = new Message();
		message.setSenderId(keyPair.getPublicKey().getId());
		message.setMessageType(Message.Type.PUBLIC_KEY);
		message.setRecipientIds(new String[] { participant.getId() });
		message.setConversationId(null);
		message.setContent(keyPair.getPublicKey().getPlainKey());
		message.sign(keyPair.getPrivateKey());
		return message;
	}

	private Message introductionMessage(PublicKey participant) {
		Message message = new Message();
		message.setSenderId(keyPair.getPublicKey().getId());
		message.setMessageType(Message.Type.INTRODUCTION);
		message.setRecipientIds(participants.toArray(new String[0]));
		message.setConversationId(conversationId);
		message.setContent(participant.getPlainKey());
		message.sign(keyPair.getPrivateKey());
		return message;
	}

	public void send(String text) {
		Message message = new Message();
		message.setSenderId(keyPair.getPublicKey().getId());
		message.setMessageType(Message.Type.PLAIN_TEXT);
		message.setRecipientIds(participants.toArray(new String[0]));
		message.setConversationId(conversationId);
		try {
			message.setAndEncryptContent(secretKey(), text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}

		message.sign(keyPair.getPrivateKey());
		messageService.send(message);
	}

	private Key getEncryptionKey(String keyId) {
		Key result = secretKeys.get(keyId);
		if (result == null) {
			result = deprecatedSecretKeys.get(keyId);
		}
		if (result == null) {
			if (keyPair.getPublicKey().getId().equals(keyId)) {
				result = keyPair.getPrivateKey();
			}
		}
		return result;
	}

	private Key secretKey() {
		if (secretKeys.isEmpty()) {
			SecretKey key = SymmetricCipher.Factory.generateSecretKey(KeyStrength.STRONGEST);
			messageService.enqueue(secretKeyMessages(key));
			addSecretKey(key);
		}
		return randomElement(secretKeys.values());
	}

	private List<Message> secretKeyMessages(SecretKey key) {
		List<Message> messages = new ArrayList<>(participants.size());
		for (String p : participants) {
			messages.add(secretKeyMessage(key, sharedPublicKeys.get(p)));
		}
		return messages;
	}

	private Message secretKeyMessage(SecretKey key, PublicKey recipient) {
		Message message = new Message();
		message.setSenderId(keyPair.getPublicKey().getId());
		message.setMessageType(Message.Type.SECRET_KEY);
		message.setRecipientIds(new String[] { recipient.getId() });
		message.setConversationId(conversationId);
		message.setAndEncryptContent(recipient, key.getPlainKey());
		message.sign(keyPair.getPrivateKey());
		return message;
	}

	private static <T> T randomElement(Collection<T> values) {
		int num = (int) (Math.random() * values.size());
		for (T t : values)
			if (--num < 0)
				return t;
		return null;
	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

}
