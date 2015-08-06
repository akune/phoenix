package de.kune.phoenix.shared;

import static de.kune.phoenix.shared.JsArrayUtil.toByteArray;
import static de.kune.phoenix.shared.JsArrayUtil.toJsArrayNumber;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;

public class CipherFactory {

	public static enum Algorithm {
		SERPENT, TWOFISH, RIJNDAEL
	}

	public static enum BlockCipherMode {
		ECB, CBC
	}

	public static enum Padding {
		/* NONE("NO_PADDING"), */RFC1321("RFC1321"), ANSIX932("ANSIX923"), ISO10126("ISO10126"), PKCS7("PKCS7");
		private String padding;

		Padding(String padding) {
			this.padding = padding;
		}

		public String getPadding() {
			return padding;
		}
	}

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

	private static class CipherJso extends JavaScriptObject implements Cipher {

		protected CipherJso() {
			// Do nothing.
		}

		@Override
		public final String getEncodedKey() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public final byte[] encrypt(byte[] plain) {
			return toByteArray((JsArrayNumber) doEncrypt(toJsArrayNumber(plain)).cast());
		}

		private final native JavaScriptObject doEncrypt(JsArrayNumber jsArrayNumber) /*-{
			return this.encrypter.execute( this.key.concat(), jsArrayNumber );
		}-*/;

		@Override
		public final byte[] decrypt(byte[] encrypted) {
			return toByteArray((JsArrayNumber) doDecrypt(toJsArrayNumber(encrypted)).cast());
		}

		private final native JavaScriptObject doDecrypt(JsArrayNumber jsArrayNumber) /*-{
			return this.decrypter.execute( this.key.concat(), jsArrayNumber );
		}-*/;

		public final static Cipher generate(String algorithm, int keySize, String mode, String padding) {
			byte[] key = new byte[keySize / 8];
			new java.util.Random().nextBytes(key);
			return create(algorithm, toJsArrayNumber(key), mode, padding);
		}

		private final static String toBase64(byte[] bytes) {
			return doToBase64(toJsArrayNumber(bytes));
		}

		private final static native String doToBase64(JsArrayNumber jsArrayNumber) /*-{
			return $wnd.base64_encode( jsArrayNumber );
		}-*/;

		public final static native Cipher create(String algorithmName, String encodedKey, String modeName,
				String paddingName) /*-{
	    	var Cipher = $wnd.__import( $wnd, "titaniumcore.crypto.Cipher" );
			var algorithm = Cipher[ algorithmName ];
			var mode  = Cipher[ modeName ];
			var padding  = Cipher[ paddingName ];
			var encrypter = Cipher.create( algorithm, Cipher[ 'ENCRYPT' ], mode, padding );
			var decrypter = Cipher.create( algorithm, Cipher[ 'DECRYPT' ], mode, padding );
			var key = $wnd.base64_decode( encodedKey );
			return { key: key, encrypter: encrypter, decrypter: decrypter };
		}-*/;

		private final static native Cipher create(String algorithmName, JsArrayNumber key, String modeName,
				String paddingName) /*-{
	    	var Cipher = $wnd.__import( $wnd, "titaniumcore.crypto.Cipher" );
			var algorithm = Cipher[ algorithmName ];
			var mode  = Cipher[ modeName ];
			var padding  = Cipher[ paddingName ];
			var encrypter = Cipher.create( algorithm, Cipher[ 'ENCRYPT' ], mode, padding );
			var decrypter = Cipher.create( algorithm, Cipher[ 'DECRYPT' ], mode, padding );
			return { key: key, encrypter: encrypter, decrypter: decrypter };
		}-*/;

	}

	public Cipher generate(Algorithm algorithm, KeyStrength strength, BlockCipherMode mode, Padding padding) {
		return CipherJso.generate(algorithm.name(), strength.getKeySize(), mode.name(), padding.getPadding());
	}

	public Cipher create(Algorithm algorithm, String encodedKey, BlockCipherMode mode, Padding padding) {
		return CipherJso.create(algorithm.name(), encodedKey, mode.name(), padding.getPadding());
	}

}
