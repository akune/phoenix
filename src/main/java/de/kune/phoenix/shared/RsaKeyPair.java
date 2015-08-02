package de.kune.phoenix.shared;

public interface RsaKeyPair {
	enum KeyType { PRIVATE, PUBLIC }
	public static enum MessageFormat {
		/**
		 * SOAEP message format.
		 */
		SOAEP, 
		/**
		 * BitPadding message format.
		 */
		BitPadding;
	}
		
	String getEncodedPublicKey();
	String getEncodedPrivateKey();
	byte[] encrypt(KeyType keyType, byte[] plain);
	byte[] decrypt(KeyType keyType, byte[] encrypted);
	int getEncryptMaxSize(KeyType keyType);
	void setMessageFormat(MessageFormat messageFormat);
}