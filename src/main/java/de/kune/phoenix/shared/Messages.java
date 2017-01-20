package de.kune.phoenix.shared;

import static de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.createPublicKey;
import static de.kune.phoenix.shared.util.ArrayUtils.contains;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Comparator;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.client.functional.Supplier;

/**
 * Provides various methods to create messages and message predicates. 
 */
public final class Messages {

	private Messages() {
		// Do nothing.
	}

	public static final Comparator<Message> SEQUENCE_KEY_ORDER = new Comparator<Message>() {
		@Override
		public int compare(Message m, Message n) {
			return m.getSequenceKey().compareTo(n.getSequenceKey());
		}
	};

	/**
	 * Creates a message predicate that checks if the message is a self-signed
	 * public key message.
	 * 
	 * @return a message predicate
	 * @see #signedPublicKey(PublicKey, KeyPair)
	 */
	public static Predicate<Message> isSelfSignedPublicKey() {
		return hasType(Message.Type.PUBLIC_KEY).and(m -> m.getKeyId() == null).and(m -> m.getConversationId() == null)
				.and(m -> {
					PublicKey extractedPublicKey = createPublicKey(m.getContent());
					GWT.log("received unencrypted public key: " + extractedPublicKey.getId());
					return (m.getSenderId().equals(extractedPublicKey.getId()) && m.checkSignature(extractedPublicKey));
				});
	}

	/**
	 * Creates a message predicate that checks if the message has the specified
	 * message type.
	 * 
	 * @param type
	 *            the message type
	 * @return a message predicate
	 */
	public static Predicate<Message> hasType(Message.Type type) {
		return m -> m.getMessageType() == type;
	}

	/**
	 * Creates a message predicate that checks if the message has the specified
	 * conversation id.
	 * 
	 * @param conversationId
	 *            the conversation id
	 * @return a message predicate
	 */
	public static Predicate<Message> hasConversationId(String conversationId) {
		return m -> conversationId.equals(m.getConversationId());
	}

	/**
	 * Creates a message predicate that checks if the message was sent by one of
	 * the supplied participants.
	 * 
	 * @param participantsSupplier
	 *            the participant supplier
	 * @return a message predicate
	 */
	public static Predicate<Message> containsSender(Supplier<Collection<String>> participantsSupplier) {
		return m -> participantsSupplier.get().contains(m.getSenderId());
	}

	/**
	 * Creates a message predicate that checks if the message was received by
	 * the server after the specified sequence key.
	 * 
	 * @param sequenceKey
	 *            the sequence key
	 * @return a message predicate
	 */
	public static Predicate<? super Message> wasReceivedAfter(String sequenceKey) {
		return m -> sequenceKey == null || sequenceKey.compareTo(m.getSequenceKey()) < 0;
	}

	/**
	 * Creates a message predicate that checks if the message contains the
	 * specified recipient.
	 * 
	 * @param recipientId
	 *            the recipient id
	 * @return a message predicate
	 */
	public static Predicate<Message> hasRecipient(String recipientId) {
		return m -> contains(m.getRecipientIds(), recipientId);
	}

	/**
	 * Creates a message predicate that checks if the message is a plain text
	 * message.
	 * 
	 * @return a message predicate
	 * @see #text(String, SecretKey, String, KeyPair, String[])
	 */
	public static Predicate<Message> isTextMessage() {
		return hasType(Message.Type.PLAIN_TEXT);
	}

	public static Predicate<Message> isParticipant() {
		return hasType(Message.Type.PARTICIPANT);
	}
	
	/**
	 * Creates a message predicate that checks if the message is a secret key
	 * message.
	 * 
	 * @return a message predicate
	 */
	public static Predicate<Message> isSecretKey() {
		return hasType(Message.Type.SECRET_KEY);
	}

	/**
	 * Creates a message predicate that checks if the message is a introduction
	 * message.
	 * 
	 * @return a message predicate
	 * @see #introduction(PublicKey, String, KeyPair, String[])
	 */
	public static Predicate<Message> isIntroduction() {
		return hasType(Message.Type.PUBLIC_KEY).and(hasConversationId());
	}

	/**
	 * Creates a message predicate that checks if the message has a conversation
	 * id.
	 * 
	 * @return a message predicate
	 */
	private static Predicate<Message> hasConversationId() {
		return m -> m.getConversationId() != null;
	}

	/**
	 * Creates a message predicate that checks if the message was sent by the
	 * specified sender.
	 * 
	 * @param senderId
	 *            the sender id
	 * @return a message predicate
	 */
	public static Predicate<Message> wasSentBy(String senderId) {
		return m -> senderId.equals(m.getSenderId());
	}

	/**
	 * Creates a public key-encrypted, signed message containing a secret key.
	 * 
	 * @param key
	 *            the key to be contained in the message to create
	 * @param conversationId
	 *            the conversation id
	 * @param sender
	 *            the sender's key pair
	 * @param recipient
	 *            the recipient's public key
	 * @return a secret key message
	 */
	public static Message secretKey(SecretKey key, String conversationId, KeyPair sender, PublicKey recipient) {
		// TODO: Produce message id from key id and conversation id, so a client
		// can query directly for a secret key message
		Message message = new Message();
		message.setSenderId(sender.getPublicKey().getId());
		message.setMessageType(Message.Type.SECRET_KEY);
		message.setRecipientIds(new String[] { recipient.getId() });
		message.setConversationId(conversationId);
		message.setAndEncryptContent(recipient, key.getPlainKey());
		message.sign(sender.getPrivateKey());
		return message;
	}

	/**
	 * Creates a message containing a signed public key to self-introduce
	 * someone to a participant who is to be invited to a conversation.
	 * 
	 * @param publicKeyToSign
	 *            the public key to sign with the message to create
	 * @param sender
	 *            the sender's key pair
	 * @return a public key message object
	 * @see #isSelfSignedPublicKey()
	 */
	public static Message signedPublicKey(PublicKey publicKeyToSign, KeyPair sender) {
		// TODO: Produce message id from sender, so a client can query directly
		// for sender's public key message
		Message message = new Message();
		message.setSenderId(sender.getPublicKey().getId());
		message.setMessageType(Message.Type.PUBLIC_KEY);
		message.setRecipientIds(new String[] { publicKeyToSign.getId() });
		message.setConversationId(null);
		message.setContent(sender.getPublicKey().getPlainKey());
		message.sign(sender.getPrivateKey());
		return message;
	}

	/**
	 * Creates a signed message introducing a new participant to a conversation.
	 * 
	 * @param introducedParticipant
	 *            the introduced participant's public key
	 * @param conversationId
	 *            the conversation id
	 * @param sender
	 *            the sender's key pair
	 * @param recipients
	 *            the recipients
	 * @return an introduction message object
	 * @see #isIntroduction()
	 */
	public static Message introduction(PublicKey introducedParticipant, String conversationId, KeyPair sender,
			String[] recipients) {
		Message message = new Message();
		message.setSenderId(sender.getPublicKey().getId());
		message.setMessageType(Message.Type.PUBLIC_KEY);
		message.setRecipientIds(recipients);
		message.setConversationId(conversationId);
		message.setContent(introducedParticipant.getPlainKey());
		message.sign(sender.getPrivateKey());
		return message;
	}

	/**
	 * Creates a signed text message.
	 * 
	 * @param text
	 *            the text
	 * @param secretKey
	 *            the secret key used for encryption
	 * @param conversationId
	 *            the conversation id
	 * @param sender
	 *            the sender's public key
	 * @param recipients
	 *            the recipients
	 * @return a text message object
	 * @see #isTextMessage()
	 */
	public static Message text(String text, SecretKey secretKey, String conversationId, KeyPair sender,
			String[] recipients) {
		Message message = new Message();
		message.setSenderId(sender.getPublicKey().getId());
		message.setMessageType(Message.Type.PLAIN_TEXT);
		message.setRecipientIds(recipients);
		message.setConversationId(conversationId);
		try {
			message.setAndEncryptContent(secretKey, text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
		message.sign(sender.getPrivateKey());
		return message;
	}

	public static Message received(Message message, KeyPair sender) {
		if (!contains(message.getRecipientIds(), sender.getPublicKey().getId())) {
			throw new IllegalArgumentException(
					"[" + sender.getPublicKey().getId() + "] is not a recipient of [" + sender + "]");
		}
		Message result = new Message();
		// TODO: Create reproducible id hash to allow caching:
		result.setId(message.getId() + sender.getPublicKey().getId() + Message.Type.RECEIVED.toString());
		result.setSenderId(sender.getPublicKey().getId());
		result.setMessageType(Message.Type.RECEIVED);
		result.setRecipientIds(message.getRecipientIds());
		result.setConversationId(message.getConversationId());
		try {
			result.setContent(message.getId().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
		result.sign(sender.getPrivateKey());
		return result;
	}
	
	public static Message participant(String screenName, SecretKey secretKey, String conversationId, KeyPair sender, String[] recipients) {
		Message message = new Message();
		message.setSenderId(sender.getPublicKey().getId());
		message.setMessageType(Message.Type.PLAIN_TEXT);
		message.setRecipientIds(recipients);
		message.setConversationId(conversationId);
		try {
			message.setAndEncryptContent(secretKey, screenName.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("utf-8 is not supported", e);
		}
		message.sign(sender.getPrivateKey());
		return message;
	}

}
