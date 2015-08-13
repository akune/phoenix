package de.kune.phoenix.client.crypto;

import static de.kune.phoenix.client.crypto.util.JsArrayUtils.toByteArray;
import static de.kune.phoenix.client.crypto.util.JsArrayUtils.toJsArrayNumber;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;

import de.kune.phoenix.client.crypto.SecretKey.KeyStrength;
import de.kune.phoenix.client.crypto.util.Base64Utils;

public interface SymmetricCipher extends Cipher {

	enum Algorithm {
		SERPENT, TWOFISH, RIJNDAEL
	}

	enum BlockCipherMode {
		ECB, CBC
	}

	enum Padding {
		/* NONE("NO_PADDING"), */RFC1321("RFC1321"), ANSIX932("ANSIX923"), ISO10126("ISO10126"), PKCS7("PKCS7");
		private String padding;

		Padding(String padding) {
			this.padding = padding;
		}

		public String getPadding() {
			return padding;
		}
	}

	class Factory {

		private static class SecretKeyImpl implements SecretKey {

			private JsArrayNumber key;

			protected SecretKeyImpl(JsArrayNumber key) {
				this.key = key;
			}

			public final static SecretKey create(byte[] key) {
				return create(toJsArrayNumber(key));
			}

			public final static SecretKey create(String base64EncodedKey) {
				return create(Base64Utils.decode(base64EncodedKey));
			}

			private final static SecretKeyImpl create(JsArrayNumber key) {
				return new SecretKeyImpl(key);
			}

			@Override
			public final String getEncodedKey() {
				return Base64Utils.encode(toByteArray(getJsArrayNumberKey()));
			}

			private final JsArrayNumber getJsArrayNumberKey() {
				return this.key;
			}

			@Override
			public final byte[] getPlainKey() {
				return toByteArray(getJsArrayNumberKey());
			}

			public final static SecretKey generate(KeyStrength keyStrength) {
				byte[] key = new byte[keyStrength.getKeySize() / 8];
				new java.util.Random().nextBytes(key);
				return create(key);
			}

		}

		private static class CipherJso extends JavaScriptObject implements SymmetricCipher {

			protected CipherJso() {
				// Do nothing.
			}

			@Override
			public final byte[] encrypt(SecretKey key, byte[] plain) {
				if (key instanceof SecretKeyImpl) {
					return toByteArray((JsArrayNumber) doEncrypt(((SecretKeyImpl) key).getJsArrayNumberKey(),
							toJsArrayNumber(plain)).cast());
				} else {
					return toByteArray(
							(JsArrayNumber) doEncrypt(toJsArrayNumber(key.getPlainKey()), toJsArrayNumber(plain))
									.cast());
				}
			}

			private final native JavaScriptObject doEncrypt(JsArrayNumber key, JsArrayNumber plain) /*-{
				return this.encrypter.execute( key.concat(), plain );
			}-*/;

			@Override
			public final byte[] decrypt(SecretKey key, byte[] encrypted) {
				if (key instanceof SecretKeyImpl) {
					return toByteArray((JsArrayNumber) doDecrypt(((SecretKeyImpl) key).getJsArrayNumberKey(),
							toJsArrayNumber(encrypted)).cast());
				} else {
					return toByteArray(
							(JsArrayNumber) doDecrypt(toJsArrayNumber(key.getPlainKey()), toJsArrayNumber(encrypted))
									.cast());
				}
			}

			private final native JavaScriptObject doDecrypt(JsArrayNumber key, JsArrayNumber jsArrayNumber) /*-{
				return this.decrypter.execute( key.concat(), jsArrayNumber );
			}-*/;

			private final static String toBase64(byte[] bytes) {
				return doToBase64(toJsArrayNumber(bytes));
			}

			private final static native String doToBase64(JsArrayNumber jsArrayNumber) /*-{
				return $wnd.base64_encode( jsArrayNumber );
			}-*/;

			public final static native SymmetricCipher create(String algorithmName, String modeName, String paddingName) /*-{
		    	var Cipher = $wnd.__import( $wnd, "titaniumcore.crypto.Cipher" );
				var algorithm = Cipher[ algorithmName ];
				var mode  = Cipher[ modeName ];
				var padding  = Cipher[ paddingName ];
				var encrypter = Cipher.create( algorithm, Cipher[ 'ENCRYPT' ], mode, padding );
				var decrypter = Cipher.create( algorithm, Cipher[ 'DECRYPT' ], mode, padding );
				return { encrypter: encrypter, decrypter: decrypter };
			}-*/;

			@Override
			public final byte[] encrypt(Key key, byte[] plain) {
				if (key instanceof SecretKey) {
					return encrypt((SecretKey) key, plain);
				}
				throw new IllegalArgumentException("key must be instance of " + SecretKey.class.getName());
			}

			@Override
			public final byte[] decrypt(Key key, byte[] encrypted) {
				if (key instanceof SecretKey) {
					return decrypt((SecretKey) key, encrypted);
				}
				throw new IllegalArgumentException("key must be instance of " + SecretKey.class.getName());
			}

		}

		public static SecretKey generateSecretKey(KeyStrength strength) {
			CipherSuite.assertReady();
			return SecretKeyImpl.generate(strength);
		}

		public static SecretKey createSecretKey(byte[] plainKey) {
			CipherSuite.assertReady();
			return SecretKeyImpl.create(plainKey);
		}

		public static SymmetricCipher createCipher(Algorithm algorithm, BlockCipherMode blockCipherMode,
				Padding padding) {
			CipherSuite.assertReady();
			return CipherJso.create(algorithm.name(), blockCipherMode.name(), padding.getPadding());
		}

		public static SecretKey createSecretKey(String encodedKey) {
			CipherSuite.assertReady();
			return SecretKeyImpl.create(encodedKey);
		}

	}

	byte[] encrypt(SecretKey key, byte[] plain);

	byte[] decrypt(SecretKey key, byte[] encrypted);

}
