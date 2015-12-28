package de.kune.phoenix.client.crypto.keystore;

import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.KeyPair;

public interface ModifiableKeyStore extends KeyStore {

	void add(Key key);

	void add(KeyPair keyPair);

}
