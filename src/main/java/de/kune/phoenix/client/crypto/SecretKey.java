package de.kune.phoenix.client.crypto;

public interface SecretKey extends Key {
	public static enum KeyStrength {
		/**
		 * Weakest key strength of 128 bit.
		 */
		WEAKEST(128),

		/**
		 * Weak key strength of 160 bit.
		 */
		WEAK(160),

		/**
		 * Medium key strength of 192 bit.
		 */
		MEDIUM(192),

		/**
		 * Strong key strength of 224 bit.
		 */
		STRONG(224),

		/**
		 * Strongest key strength of 256 bit.
		 */
		STRONGEST(256);
		private int keySize;

		KeyStrength(int keySize) {
			this.keySize = keySize;
		}

		public int getKeySize() {
			return keySize;
		}
	}

}