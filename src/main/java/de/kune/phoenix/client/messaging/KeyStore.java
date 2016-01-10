package de.kune.phoenix.client.messaging;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.SecretKey;

public interface KeyStore<T> {

	public static class DeprecatingSecretKeyStore implements KeyStore<SecretKey> {
		private static final int KEY_LIFESPAN_MILLIS = 10 * 1000;

		private final KeyPair keyPair;
		private final Map<String, SecretKey> secretKeys = new HashMap<>();
		private final Map<String, SecretKey> deprecatedSecretKeys = new HashMap<>();

		public DeprecatingSecretKeyStore(KeyPair keyPair) {
			this.keyPair = keyPair;
		}

		@Override
		public void addKey(SecretKey key) {
			secretKeys.put(key.getId(), key);
			new Timer() {
				@Override
				public void run() {
					deprecateKey(key);
				}

			}.schedule(KEY_LIFESPAN_MILLIS);
		}

		@Override
		public void deprecateAllKeys() {
			while (!secretKeys.isEmpty()) {
				SecretKey key = secretKeys.values().iterator().next();
				deprecateKey(key);
			}
		}

		private void deprecateKey(SecretKey key) {
			GWT.log("deprecating secret key [" + key.getId() + "]");
			deprecatedSecretKeys.put(key.getId(), key);
			secretKeys.remove(key.getId());
		}

		@Override
		public Key getKey(String keyId) {
			Key result = secretKeys.get(keyId);
			if (result == null) {
				result = deprecatedSecretKeys.get(keyId);
			}
			if (result == null) {
				if (keyPair.getPublicKey().getId().equals(keyId)) {
					result = keyPair.getPrivateKey();
				}
			}
			return result;
		}

		@Override
		public boolean containsValidKey() {
			return secretKeys.isEmpty();
		}

		@Override
		public SecretKey anyValidKey() {
			return randomElement(secretKeys.values());
		}

		private static <T> T randomElement(Collection<T> values) {
			int num = (int) (Math.random() * values.size());
			for (T t : values)
				if (--num < 0)
					return t;
			return null;
		}

		@Override
		public KeyPair getKeyPair() {
			return keyPair;
		}

	}

	void addKey(T key);

	void deprecateAllKeys();

	Key getKey(String keyId);

	boolean containsValidKey();

	T anyValidKey();

	KeyPair getKeyPair();
}
