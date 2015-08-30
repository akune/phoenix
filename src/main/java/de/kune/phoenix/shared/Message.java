package de.kune.phoenix.shared;

import java.util.Arrays;
import java.util.Date;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.Cipher;
import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.PrivateKey;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.crypto.SymmetricCipher;
import de.kune.phoenix.client.crypto.util.Base64Utils;
import de.kune.phoenix.client.crypto.util.Digest;
import de.kune.phoenix.client.crypto.util.Sha256;

public class Message {

	public static enum Type {
		/**
		 * Plain text message.
		 */
		PLAIN_TEXT,

		/**
		 * Secret key message.
		 */
		SECRET_KEY,

		/**
		 * Invitation message.
		 */
		INVITATION,

		/**
		 * Introduction message.
		 */
		INTRODUCTION,
	}

	/**
	 * The id of the sender of this message.
	 */
	private String senderId;
	/**
	 * The ids of the recipients of this message.
	 */
	private String[] recipientIds;
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
	 * The time stamp when this message was received by the first transmitting
	 * server node.
	 */
	private Date transmission;

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
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

	public Date getTransmission() {
		return transmission;
	}

	public void setTransmission(Date transmission) {
		this.transmission = transmission;
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
	}

	public byte[] getDecryptedContent(Key key) {
		return getDecryptionCipher(key).decrypt(key, getContent());
	}

	private byte[] createDigest(byte[] salt) {
//		GWT.log("digesting...");
		Digest d = new Sha256();
//		GWT.log("...salting: " + Arrays.toString(salt));
		d = d.feed(salt);
		if (getContent() != null) {
//			GWT.log("...content: " + Arrays.toString(content));
			d = d.feed(getContent());
		}
//		GWT.log("...timestamp: " + timestamp == null ? "" : Long.toString(timestamp.getTime()));
		d = d.feed(timestamp == null ? "" : Long.toString(timestamp.getTime()));
		if (getKeyId() != null) {
			d = d.feed(getKeyId());
		}
		if (getSenderId() != null) {
//			GWT.log("...sender: " + getSenderId());
			d = d.feed(getSenderId());
		}
		if (recipientIds != null) {
			for (String recipientId : getRecipientIds()) {
//				GWT.log("...recipient: " + recipientId);
				d = d.feed(recipientId);
			}
		}
		if (getMessageType() != null) {
//			GWT.log("...message type: " + messageType.name());
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
		return "Message [senderId=" + senderId + ", recipientIds=" + Arrays.toString(recipientIds) + ", keyId=" + keyId
				+ ", messageType=" + messageType + ", content=" + Base64Utils.encode(content) + ", timestamp=" + timestamp
				+ ", signature=" + Base64Utils.encode(signature) + ", transmission=" + transmission + "]";
	}

}
