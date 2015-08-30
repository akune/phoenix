package de.kune.phoenix.client.crypto;

public interface MutableKeyStore extends KeyStore {

	void add(Key key);

	void add(KeyPair keyPair);

}
