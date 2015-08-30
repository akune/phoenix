package de.kune.phoenix.client.crypto.util;

public interface ByteArraySource {

	byte[] toByteArray();
	
	byte[] toByteArray(byte[] salt);

	String toBase64();
	
	String toBase64(byte[] salt);

}
