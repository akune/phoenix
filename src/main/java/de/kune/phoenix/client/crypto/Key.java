package de.kune.phoenix.client.crypto;

public interface Key {
	String getId();
	String getEncodedKey();
	byte[] getPlainKey();
}
