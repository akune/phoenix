package de.kune.phoenix.client.messaging;

import static de.kune.phoenix.client.functional.Predicate.always;
import static de.kune.phoenix.client.functional.Predicate.hasType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.functional.ConversationInitiationHandler;
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
		messageService.addMessageHandler(hasType(Message.Type.PUBLIC_KEY), this::handlePublicKeyMessage);
		messageService.addMessageHandler(always(), this::validateSignature);
		messageService.addMessageHandler(hasType(Message.Type.INTRODUCTION), this::handleIntroductionToNewConversation);
	}

	private void handleIntroductionToNewConversation(Message message, byte[] data) {
		GWT.log("received introduction: " + message);
		if (messageContainsPublicKeyOfThisClientSession(message) || messageWasSentByThisClientSession(message)) {
			if (messageHasUnknownConversationId(message)) {
				GWT.log("received introduction to new conversation. participants: "
						+ Arrays.toString(message.getRecipientIds()));
				PublicKey extractedPublicKey = AsymmetricCipher.Factory.createPublicKey(message.getContent());
				sharedPublicKeys.put(extractedPublicKey.getId(), extractedPublicKey);
				Conversation.Builder builder = Conversation.builder().keyPair(keyPair)
						.sharedPublicKeys(sharedPublicKeys).conversationId(message.getConversationId())
						.recipientIds(message.getRecipientIds());
				conversationInitiationHandler.handle(builder);
				conversations.put(message.getConversationId(), builder.build());
			}
		}
	}

	private boolean messageHasUnknownConversationId(Message message) {
		return !conversations.containsKey(message.getConversationId());
	}

	private boolean messageWasSentByThisClientSession(Message message) {
		return recipientId.equals(message.getSenderId());
	}

	private boolean messageContainsPublicKeyOfThisClientSession(Message message) {
		return Arrays.equals(message.getContent(), keyPair.getPublicKey().getPlainKey());
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

	private void handlePublicKeyMessage(Message message, byte[] data) {
		if (message.getKeyId() == null) {
			PublicKey extractedPublicKey = AsymmetricCipher.Factory.createPublicKey(message.getContent());
			GWT.log("received unencrypted public key: " + extractedPublicKey.getId());
			if (message.getSenderId().equals(extractedPublicKey.getId())
					&& message.checkSignature(extractedPublicKey)) {
				GWT.log("adding self-signed public key to shared public keys");
				sharedPublicKeys.put(extractedPublicKey.getId(), extractedPublicKey);
			}
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

	public Conversation getConversation(String conversationId) {
		return conversations.get(conversationId);
	}

}
