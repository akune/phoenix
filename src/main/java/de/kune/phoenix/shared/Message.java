package de.kune.phoenix.shared;

import static de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.createPublicKey;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.Cipher;
import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PrivateKey;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.crypto.SymmetricCipher;
import de.kune.phoenix.client.crypto.util.Base64Utils;
import de.kune.phoenix.client.crypto.util.Digest;
import de.kune.phoenix.client.crypto.util.Sha256;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.client.functional.Supplier;

public class Message implements Identifiable<String> {

	public static enum Type {
		/**
		 * Plain text message type.
		 */
		PLAIN_TEXT,

		/**
		 * Secret key message type.
		 */
		SECRET_KEY,

		/**
		 * Public key message type.
		 */
		PUBLIC_KEY,

	}

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
	 * Creates a message predicate that checks if the message is a plain text
	 * message.
	 * 
	 * @return a message predicate
	 * @see #text(String, SecretKey, String, KeyPair, String[])
	 */
	public static Predicate<Message> isTextMessage() {
		return hasType(Message.Type.PLAIN_TEXT);
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

	/**
	 * The id of this message.
	 */
	private String id;
	/**
	 * The id of the sender of this message.
	 */
	private String senderId;
	/**
	 * The ids of the recipients of this message.
	 */
	private String[] recipientIds;
	/**
	 * The id of the conversation this message was sent to.
	 */
	private String conversationId;
	/**
	 * The id of the key this message is encrypted with.
	 */
	private String keyId;
	/**
	 * The type of this message.
	 */
	private Type messageType;
	/**
	 * The encrypted payload of this message.
	 */
	private byte[] content;
	/**
	 * The time stamp of this message.
	 */
	private Date timestamp;
	/**
	 * The sender's signature of this message.
	 */
	private byte[] signature;
	/**
	 * The sequence key string. An element of a ascending sequence strictly
	 * ordering all messages received by a certain node. TODO: Consider using
	 * message chaining instead.
	 */
	private String sequenceKey;

	public Message() {
		this.id = Identifiable.generateStringId(32);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public String[] getRecipientIds() {
		return recipientIds;
	}

	public void setRecipientIds(String[] recipientIds) {
		this.recipientIds = recipientIds;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public Type getMessageType() {
		return messageType;
	}

	public void setMessageType(Type messageType) {
		this.messageType = messageType;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public String getSequenceKey() {
		return sequenceKey;
	}

	public void setSequenceKey(String transmission) {
		this.sequenceKey = transmission;
	}

	private Cipher getEncryptionCipher(Key key) {
		if (key instanceof SecretKey) {
			return SymmetricCipher.Factory.createCipher(SymmetricCipher.Algorithm.RIJNDAEL,
					SymmetricCipher.BlockCipherMode.ECB, SymmetricCipher.Padding.PKCS7);
		} else if (key instanceof PublicKey) {
			return AsymmetricCipher.Factory.createCipher(AsymmetricCipher.MessageFormat.SOAEP);
		} else {
			throw new IllegalArgumentException("key must be instanceof " + SecretKey.class + " or " + PublicKey.class);
		}
	}

	private Cipher getDecryptionCipher(Key key) {
		if (key instanceof SecretKey) {
			return SymmetricCipher.Factory.createCipher(SymmetricCipher.Algorithm.RIJNDAEL,
					SymmetricCipher.BlockCipherMode.ECB, SymmetricCipher.Padding.PKCS7);
		} else if (key instanceof PrivateKey) {
			return AsymmetricCipher.Factory.createCipher(AsymmetricCipher.MessageFormat.SOAEP);
		} else {
			throw new IllegalArgumentException("key must be instanceof " + SecretKey.class + " or " + PrivateKey.class);
		}
	}

	public void setAndEncryptContent(Key key, byte[] content) {
		setContent(getEncryptionCipher(key).encrypt(key, content));
		setKeyId(key.getId());
	}

	public byte[] getDecryptedContent(Key key) {
		return getDecryptionCipher(key).decrypt(key, getContent());
	}

	private byte[] createDigest(byte[] salt) {
		Digest d = new Sha256();
		d = d.feed(salt);
		if (getId() != null) {
			d = d.feed(getId());
		}
		if (getContent() != null) {
			d = d.feed(getContent());
		}
		d = d.feed(timestamp == null ? "" : Long.toString(timestamp.getTime()));
		if (getKeyId() != null) {
			d = d.feed(getKeyId());
		}
		if (getSenderId() != null) {
			d = d.feed(getSenderId());
		}
		if (getConversationId() != null) {
			d = d.feed(getConversationId());
		}
		if (recipientIds != null) {
			for (String recipientId : getRecipientIds()) {
				d = d.feed(recipientId);
			}
		}
		if (getMessageType() != null) {
			d = d.feed(messageType.name());
		}
		return d.iterate(15).toByteArray(salt);
	}

	public boolean checkSignature(PublicKey publicKey) {
		if (!senderId.equals(publicKey.getId())) {
			throw new IllegalArgumentException("this message was not sent by <" + publicKey.getId() + ">");
		}
		AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(AsymmetricCipher.MessageFormat.SOAEP);
		byte[] decryptedDigest = cipher.decrypt(publicKey, signature);
		byte[] salt = Arrays.copyOfRange(decryptedDigest, 0, decryptedDigest.length - 256 / 8);
		byte[] digest = createDigest(salt);
		return Arrays.equals(decryptedDigest, digest);
	}

	public void sign(PrivateKey privateKey) {
		AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(AsymmetricCipher.MessageFormat.SOAEP);
		byte[] salt = new byte[5];
		new java.util.Random().nextBytes(salt);
		this.signature = cipher.encrypt(privateKey, createDigest(salt));
	}

	@Override
	public String toString() {
		return "Message[" + messageType + "] [id=" + id + ", senderId=" + senderId + ", conversationId="
				+ conversationId + ", recipientIds=" + Arrays.toString(recipientIds) + ", keyId=" + keyId + ", content="
				+ Base64Utils.encode(content) + ", timestamp=" + timestamp + ", signature="
				+ Base64Utils.encode(signature) + ", transmission=" + sequenceKey + "]";
	}

}
