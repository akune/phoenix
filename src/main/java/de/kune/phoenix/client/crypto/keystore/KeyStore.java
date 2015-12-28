package de.kune.phoenix.client.crypto.keystore;

import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.PublicKey;

public interface KeyStore {

	Key getDecryptionKey(String encryptionKeyId);
	PublicKey getPublicKey(String publicKeyId);
	
}
