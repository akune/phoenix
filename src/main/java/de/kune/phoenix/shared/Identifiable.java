package de.kune.phoenix.shared;

import java.util.Random;

public interface Identifiable<T> {

	static byte[] generateId(int length) {
		final int BYTES = 8; 
		byte[] id = new byte[length];
		byte[] random = new byte[length - BYTES];
		new Random().nextBytes(random);
		for (int i = 0; i < random.length; i++) {
			id[i] = random[i];
		}
		long time = System.currentTimeMillis();
		for (int i = 0; i < BYTES; i++) {
			int pos = id.length - i - 1;
			if (pos >=0) {
				id[pos] = (byte) (time >> i);
			}
		}
		return id;
	}
	
	public T getId();
	
}
