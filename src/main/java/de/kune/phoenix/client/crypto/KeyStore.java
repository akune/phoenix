package de.kune.phoenix.client.crypto;

public interface KeyStore {

	Key getDecryptionKey(String encryptionKeyId);
	PublicKey getPublicKey(String publicKeyId);
	
}
