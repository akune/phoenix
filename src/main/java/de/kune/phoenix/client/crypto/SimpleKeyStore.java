package de.kune.phoenix.client.crypto;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SimpleKeyStore implements MutableKeyStore {

	private Map<String, SecretKey> secretKeys = new HashMap<String, SecretKey>();
	private Map<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();
	private Set<KeyPair> keyPairs = new HashSet<KeyPair>();
	private KeyStore parent;

	public SimpleKeyStore(KeyStore parent) {
		this.parent = parent;
	}

	public SimpleKeyStore() {

	}

	@Override
	public void add(Key key) {
		if (key instanceof SecretKey) {
			secretKeys.put(key.getId(), (SecretKey) key);
		} else if (key instanceof PublicKey) {
			publicKeys.put(key.getId(), (PublicKey) key);
		}
	}

	@Override
	public void add(KeyPair keyPair) {
		keyPairs.add(keyPair);
	}

	@Override
	public Key getDecryptionKey(String encryptionKeyId) {
		Key result = secretKeys.get(encryptionKeyId);
		if (result == null) {
			for (KeyPair keyPair : keyPairs) {
				if (keyPair.getPrivateKey().getId().equals(encryptionKeyId)) {
					return keyPair.getPublicKey();
				}
				if (keyPair.getPublicKey().getId().equals(encryptionKeyId)) {
					return keyPair.getPrivateKey();
				}
			}
		}
		if (result == null && parent != null) {
			result = parent.getDecryptionKey(encryptionKeyId);
		}
		return result;
	}

	@Override
	public PublicKey getPublicKey(String publicKeyId) {
		PublicKey result = publicKeys.get(publicKeyId);
		if (result == null && parent != null) {
			result = parent.getPublicKey(publicKeyId);
		}
		return result;
	}
	
	public SecretKey getAnySecretKey() {
		if (secretKeys.isEmpty()) {
			return null;
		}
		return secretKeys.values().iterator().next();
	}

	public Collection<SecretKey> getAllSecretKeys() {
		return new LinkedHashSet<SecretKey>(secretKeys.values());
	}

}
