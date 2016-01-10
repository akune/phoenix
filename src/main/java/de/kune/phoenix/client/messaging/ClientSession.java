package de.kune.phoenix.client.messaging;

import static de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.createPublicKey;
import static de.kune.phoenix.client.functional.Predicate.always;
import static de.kune.phoenix.client.functional.Predicate.hasType;
import static de.kune.phoenix.shared.Message.isSelfSignedPublicKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.functional.ConversationInitiationHandler;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.shared.Identifiable;
import de.kune.phoenix.shared.Message;

public class ClientSession {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private KeyPair keyPair;
		private ConversationInitiationHandler conversationInitiationHandler;

		public Builder keyPair(KeyPair keyPair) {
			this.keyPair = keyPair;
			return this;
		}

		public Builder conversationInitiationHandler(ConversationInitiationHandler conversationInitiationHandler) {
			this.conversationInitiationHandler = conversationInitiationHandler;
			return this;
		}

		public ClientSession build() {
			ClientSession clientSession = new ClientSession(keyPair, conversationInitiationHandler);
			clientSession.messageService.start(clientSession.recipientId);
			return clientSession;
		}
	}

	private final MessageService messageService = MessageService.instance();
	private final KeyPair keyPair;
	private final Map<String, PublicKey> sharedPublicKeys = new HashMap<>();
	private final ConversationInitiationHandler conversationInitiationHandler;
	private final String recipientId;
	private Map<String, Conversation> conversations = new HashMap<>();

	private ClientSession(KeyPair keyPair, ConversationInitiationHandler conversationInitiationHandler) {
		this.keyPair = keyPair;
		this.conversationInitiationHandler = conversationInitiationHandler;
		sharedPublicKeys.put(keyPair.getPublicKey().getId(), keyPair.getPublicKey());
		recipientId = keyPair.getPublicKey().getId();
		messageService.addMessageHandler(isSelfSignedPublicKey(), this::handlePublicKeyMessage);
		messageService.addMessageHandler(always(), this::validateSignature);
		messageService.addMessageHandler(isIntroductionToNewConversation(), this::handleIntroductionToNewConversation);
	}

	private Predicate<Message> isIntroductionToNewConversation() {
		return hasType(Message.Type.INTRODUCTION).and(isPublicKeyOfThisClientSession().or(wasSentByThisClientSession()))
				.and(hasUnknownConversationId());
	}

	private void handlePublicKeyMessage(Message message, byte[] data) {
		addPublicKey(createPublicKey(message.getContent()));
	}

	private void addPublicKey(PublicKey publicKey) {
		sharedPublicKeys.put(publicKey.getId(), publicKey);
	}

	private void handleIntroductionToNewConversation(Message message, byte[] data) {
		addPublicKey(createPublicKey(message.getContent()));
		Conversation.Builder builder = Conversation.builder().keyPair(keyPair).sharedPublicKeys(sharedPublicKeys)
				.conversationId(message.getConversationId()).recipientIds(message.getRecipientIds());
		conversationInitiationHandler.handle(builder);
		conversations.put(message.getConversationId(), builder.build());
	}

	private Predicate<Message> hasUnknownConversationId() {
		return m -> !conversations.containsKey(m.getConversationId());
	}

	private Predicate<Message> wasSentByThisClientSession() {
		return m -> recipientId.equals(m.getSenderId());
	}

	private Predicate<Message> isPublicKeyOfThisClientSession() {
		return m -> Arrays.equals(m.getContent(), keyPair.getPublicKey().getPlainKey());
	}

	private void validateSignature(Message message, byte[] data) {
		if (message.getSignature() == null) {
			throw new IllegalStateException("unsigned message [" + message + "]");
		}
		PublicKey publicKey = getPublicKey(message);
		if (publicKey == null) {
			throw new IllegalStateException("unknown public key [" + message.getSenderId() + "]");
		}
		if (!message.checkSignature(publicKey)) {
			throw new IllegalStateException("incorrectly signed message [" + message + "]");
		}
	}

	private PublicKey getPublicKey(Message message) {
		PublicKey publicKey = sharedPublicKeys.get(message.getSenderId());
		if (publicKey != null && !publicKey.getId().equals(message.getSenderId())) {
			throw new IllegalStateException("obtained public key does not match id");
		}
		return publicKey;
	}

	public Conversation startConversation() {
		Conversation.Builder builder = Conversation.builder().keyPair(keyPair).sharedPublicKeys(sharedPublicKeys)
				.conversationId(Identifiable.generateStringId(15)).recipientIds(keyPair.getPublicKey().getId());
		conversationInitiationHandler.handle(builder);
		Conversation conversation = builder.build();
		conversations.put(conversation.getConversationId(), conversation);
		return conversation;
	}

	/**
	 * Returns the conversation with the specified id if it is registered with
	 * this client session or null.
	 * 
	 * @param conversationId
	 *            the conversation id
	 * @return a conversation object or null
	 */
	public Conversation getConversation(String conversationId) {
		return conversations.get(conversationId);
	}

}
