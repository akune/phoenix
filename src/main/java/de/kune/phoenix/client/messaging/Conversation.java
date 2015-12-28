package de.kune.phoenix.client.messaging;

import static de.kune.phoenix.client.functional.Predicate.containsSender;
import static de.kune.phoenix.client.functional.Predicate.hasConversationId;
import static de.kune.phoenix.client.functional.Predicate.hasType;
import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.functional.MessageHandler;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.shared.Message;

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

	private final KeyPair keyPair;
	private final MessageService messageService = MessageService.instance();
	private final String conversationId;
	private final Map<String, PublicKey> sharedPublicKeys;
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
		// TODO: Decrypt message
		// TODO: Add to secret key store
	}

	private void handleIntroduction(Message message, byte[] data) {
		GWT.log("received introduction of new participant to conversation [" + conversationId + "]");
		PublicKey extractedPublicKey = AsymmetricCipher.Factory.createPublicKey(message.getContent());
		sharedPublicKeys.put(extractedPublicKey.getId(), extractedPublicKey);
		// TODO: Invalidate all secret keys
		// TODO: Add new participant (and his public key)
	}

	private void handleTextMessage(Message message, byte[] data) {
		GWT.log(this + "received text message: " + message);
		// TODO: Decrypt message
		try {
			GWT.log("contained text: " + new String(message.getContent(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
		receivedMessageHandler.handleReceivedMessage(message, message.getContent());
		// TODO: Inform GUI
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
		// TODO: Encrypt message
		Message message = new Message();
		message.setSenderId(keyPair.getPublicKey().getId());
		message.setMessageType(Message.Type.PLAIN_TEXT);
		message.setRecipientIds(participants.toArray(new String[0]));
		message.setConversationId(conversationId);
		try {
			message.setContent(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
		message.sign(keyPair.getPrivateKey());
		messageService.send(message);
	}

}
