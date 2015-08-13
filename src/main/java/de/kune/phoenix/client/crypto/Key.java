package de.kune.phoenix.client.crypto;

public interface Key {
	String getEncodedKey();
	byte[] getPlainKey();
}
