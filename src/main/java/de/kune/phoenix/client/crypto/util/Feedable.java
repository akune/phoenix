package de.kune.phoenix.client.crypto.util;

import java.io.UnsupportedEncodingException;

public interface Feedable<T> {

	T feed(String input);

	T feed(String input, String charset) throws UnsupportedEncodingException;

	T feed(byte[] data);

}
