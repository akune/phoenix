package de.kune.phoenix.shared;

public interface Cipher {

	String getEncodedKey();
	byte[] encrypt(byte[] plain);
	byte[] decrypt(byte[] encrypted);

}
