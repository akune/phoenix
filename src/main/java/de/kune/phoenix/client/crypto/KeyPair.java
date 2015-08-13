package de.kune.phoenix.client.crypto;

public interface KeyPair {

	public static enum PublicExponent {
		/**
		 * Smallest public exponent 3.
		 */
		SMALLEST(3),

		/**
		 * Small public exponent 7.
		 */
		SMALL(7),

		/**
		 * Medium public exponent 17.
		 */
		MEDIUM(17),

		/**
		 * Big public exponent 257.
		 */
		BIG(257),

		/**
		 * Biggest public exponent 65537.
		 */
		BIGGEST(65537);
		private int exponent;

		PublicExponent(int exponent) {
			this.exponent = exponent;
		}

		public int getExponent() {
			return exponent;
		}
	}

	public static enum KeyStrength {

		/**
		 * Weakest key strength of 256 bit.
		 */
		WEAKEST(256),

		/**
		 * Weak key strength of 512 bit.
		 */
		WEAK(512),

		/**
		 * Medium key strength of 1024 bit.
		 */
		MEDIUM(1024),

		/**
		 * Strong key strength of 2048 bit.
		 */
		STRONG(2048),

		/**
		 * Strongest key strength of 4096 bit.
		 */
		STRONGEST(4096);
		private int keySize;

		KeyStrength(int keySize) {
			this.keySize = keySize;
		}

		public int getKeySize() {
			return keySize;
		}
	}

	PublicKey getPublicKey();

	PrivateKey getPrivateKey();

}
