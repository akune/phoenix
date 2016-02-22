package de.kune.phoenix.client.messaging;

import static de.kune.phoenix.shared.Message.containsSender;
import static de.kune.phoenix.shared.Message.hasConversationId;
import static de.kune.phoenix.shared.Message.isIntroduction;
import static de.kune.phoenix.shared.Message.isSecretKey;
import static de.kune.phoenix.shared.Message.isTextMessage;
import static de.kune.phoenix.shared.Message.received;
import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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
import de.kune.phoenix.client.messaging.KeyStore.DeprecatingSecretKeyStore;
import de.kune.phoenix.shared.Message;
import de.kune.phoenix.shared.Message.Type;

public class Conversation {

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

	private SecretKey secretKey() {
		if (secretKeyStore.containsValidKey()) {
			SecretKey key = SymmetricCipher.Factory.generateSecretKey(KeyStrength.STRONGEST);
			messageService.enqueue(secretKeyMessages(key));
			secretKeyStore.addKey(key);
		}
		return secretKeyStore.anyValidKey();
	}

	private final MessageService messageService = MessageService.instance();
	private final String conversationId;
	private final Map<String, PublicKey> sharedPublicKeys;
	private Set<String> participants = new HashSet<>();
	private final KeyStore<SecretKey> secretKeyStore;
	private final MessageHandler receivedMessageHandler;

	private Conversation(KeyPair keyPair, Map<String, PublicKey> sharedPublicKeys, String conversationId,
			String[] recipientIds, MessageHandler receivedMessageHandler) {
		this.secretKeyStore = new DeprecatingSecretKeyStore(keyPair);
		this.sharedPublicKeys = sharedPublicKeys;
		this.conversationId = conversationId;
		this.receivedMessageHandler = receivedMessageHandler;
		participants.addAll(Arrays.asList(recipientIds));
		messageService.addMessageHandler(isFromValidSenderToMe().and(isSecretKey()), this::handleSecretKey);
		messageService.addMessageHandler(isFromValidSenderToMe().and(isIntroduction()), this::handleIntroduction);
		messageService.addMessageHandler(isFromValidSenderToMe().and(isTextMessage()), this::handleTextMessage);
		messageService.addMessageHandler(isFromValidSenderToMe().and(isReceiveConfirmation()),
				this::handleReceiveConfirmation);
	}

	private Predicate<? super Message> isReceiveConfirmation() {
		return Message.hasType(Type.RECEIVED);
	}

	private Predicate<Message> isFromValidSenderToMe() {
		return hasConversationId(conversationId).and(containsSender(() -> participants));
	}

	public String getConversationId() {
		return conversationId;
	}

	private void handleSecretKey(Message message, byte[] data) {
		GWT.log("received secret key message: " + message);
		decrypt(message, (m, c) -> secretKeyStore.addKey(SymmetricCipher.Factory.createSecretKey(c)));
	}

	private void handleIntroduction(Message message, byte[] data) {
		GWT.log("received introduction of new participant to conversation [" + conversationId + "]");
		PublicKey extractedPublicKey = AsymmetricCipher.Factory.createPublicKey(message.getContent());
		sharedPublicKeys.put(extractedPublicKey.getId(), extractedPublicKey);
		participants.add(extractedPublicKey.getId());
		secretKeyStore.deprecateAllKeys();
	}

	private List<String> sentReceiveConfirmations = new ArrayList<>();

	private void handleReceiveConfirmation(Message message, byte[] data) {
		try {
			String messageId = new String(message.getContent(), "UTF-8");
			String recipient = message.getSenderId();
			if (secretKeyStore.getKeyPair().getPublicKey().getId().equals(recipient)) {
				sentReceiveConfirmations.add(messageId);
			}
			GWT.log("message [" + messageId + "] was received by [" + message.getSenderId() + "]");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
	}

	private void handleTextMessage(Message message, byte[] data) {
		GWT.log("received text message: " + message);
		decrypt(message, (m, c) -> receivedMessageHandler.handleReceivedMessage(m, c));
		new Timer() {
			public void run() {
				if (!sentReceiveConfirmations.contains(message.getId())) {
					messageService.send(received(message, secretKeyStore.getKeyPair()));
				}
			}
		}.schedule(100);
	}

	private void decrypt(Message message, MessageHandler messageHandler) {
		if (message.getKeyId() == null) {
			throw new IllegalStateException("tried to decrypt unencrypted message");
		}
		Key encryptionKey = secretKeyStore.getKey(message.getKeyId());
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
		return Message.signedPublicKey(participant, secretKeyStore.getKeyPair());
	}

	private Message introductionMessage(PublicKey participant) {
		return Message.introduction(participant, conversationId, secretKeyStore.getKeyPair(),
				participants.toArray(new String[0]));
	}

	public Message send(String text) {
		return messageService.send(Message.text(text, secretKey(), conversationId, secretKeyStore.getKeyPair(),
				participants.toArray(new String[0])));
	}

	private List<Message> secretKeyMessages(SecretKey key) {
		List<Message> messages = new ArrayList<>(participants.size());
		for (String p : participants) {
			messages.add(secretKeyMessage(key, sharedPublicKeys.get(p)));
		}
		return messages;
	}

	private Message secretKeyMessage(SecretKey key, PublicKey recipient) {
		return Message.secretKey(key, conversationId, secretKeyStore.getKeyPair(), recipient);
	}

	public String getSenderId() {
		return secretKeyStore.getKeyPair().getPublicKey().getId();
	}

}
