package de.kune.phoenix.client.crypto;

import java.util.Arrays;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl.PrivateKeyImpl;
import de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl.PublicKeyImpl;
import de.kune.phoenix.client.crypto.KeyPair.KeyStrength;
import de.kune.phoenix.client.crypto.KeyPair.PublicExponent;
import de.kune.phoenix.client.crypto.util.JsArrayUtils;

public interface AsymmetricCipher extends Cipher {

	enum MessageFormat {
		/**
		 * SOAEP message format.
		 */
		SOAEP,

		/**
		 * BitPadding message format.
		 */
		BitPadding;
	}

	class Factory {

		protected static class KeyPairImpl implements KeyPair {

			private final PublicKey publicKey;
			private final PrivateKey privateKey;

			protected static class PrivateKeyImpl extends EncodedKey implements PrivateKey {
				protected PrivateKeyImpl(String key) {
					super(key);
				}
			}

			protected static class PublicKeyImpl extends EncodedKey implements PublicKey {
				protected PublicKeyImpl(String key) {
					super(key);
				}
			}

			public KeyPairImpl(PublicKey publicKey, PrivateKey privateKey) {
				this.publicKey = publicKey;
				this.privateKey = privateKey;
			}

			@Override
			public final PrivateKey getPrivateKey() {
				return privateKey;
			}

			@Override
			public final PublicKey getPublicKey() {
				return publicKey;
			}

		}

		public static void generateKeyPairAsync(final KeyStrength strength, final PublicExponent exponent,
				final Callback<KeyPair, Void> doneCallback, final Callback<Integer, Void> progressCallback) {
			doGenerateKeyPairAsync(strength.getKeySize(), exponent.getExponent(), doneCallback, progressCallback);
		}

		private static native int doGenerateKeyPairAsync(int keySize, int exponent, Callback<KeyPair, Void> c,
				Callback<Integer, Void> progressCallback) /*-{
			$wnd.__unit( "KeyPairGenerator" );
			$wnd.__uses( "packages.js" );
			$wnd.__uses( "BigInteger.init1.js" );
			$wnd.__uses( "BigInteger.init2.js" );
			$wnd.__uses( "RSA.init1.js" );
			$wnd.__uses( "RSA.init2.js" );
			$wnd.__uses( "RSA.init3.js" );
			$wnd.__uses( "RSAKeyFormat.js" );
			$wnd.__uses( "RSAMessageFormat.js" );
			$wnd.__uses( "RSAMessageFormatSOAEP.js" );
			$wnd.__uses( "RSAMessageFormatBitPadding.js" );
			
			var BigInteger = $wnd.__import( $wnd,"titaniumcore.crypto.BigInteger" );
			var RSA = $wnd.__import( $wnd,"titaniumcore.crypto.RSA" );
			var RSAMessageFormatSOAEP = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatSOAEP" );
			var RSAMessageFormatBitPadding = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatBitPadding" );
			var RSAKeyFormat = $wnd.__import( $wnd, "titaniumcore.crypto.RSAKeyFormat" );
			RSA.installKeyFormat( RSAKeyFormat );
			RSA.installMessageFormat( RSAMessageFormatSOAEP );
			
			var progress = function(c){
				progressCallback.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)(@ja‌​va.lang.Integer::valueOf(Ljava/lang/String;)('' + c));
			}
			var result = function(result){
			console.log('result:');
			console.log(arguments);
console.log('->1');
var encodedPublicKey = $wnd.base64_encode(result.publicKeyBytes());
var encodedPrivateKey = $wnd.base64_encode(result.privateKeyBytes());
console.log( encodedPublicKey );
console.log( encodedPrivateKey );
var publicKey = @de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl.PublicKeyImpl::new(Ljava/lang/String;)(encodedPublicKey);
var privateKey = @de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl.PrivateKeyImpl::new(Ljava/lang/String;)(encodedPrivateKey);
console.log('->2');
var keyPair = @de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl::new(Lde/kune/phoenix/client/crypto/PublicKey;Lde/kune/phoenix/client/crypto/PrivateKey;)(publicKey,privateKey);
//var keyPair = @de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.KeyPairImpl::new(Ljava/lang/String;Ljava/lang/String;)(encodedPublicKey,encodedPrivateKey);
console.log( keyPair );
console.log('->3');
				c.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)(keyPair);
			};
			var done = function(result) {};
			var rsa = new RSA();
			rsa.messageFormat = RSAMessageFormatSOAEP;
			return rsa.generateAsync( keySize, exponent, progress, result, done );
		}-*/;

		private static class AsymmetricCipherImpl implements AsymmetricCipher {

			private RsaJso rsa;

			private static class RsaJso extends JavaScriptObject {
				protected RsaJso() {
					// Do nothing.
				}

				private final native JavaScriptObject publicKeyEncrypt(String publicKey, JsArrayNumber plain) /*-{
				console.log('key: ' + publicKey);
				console.log('decoded key: ' + $wnd.base64_decode(publicKey));
				console.log('encrypting: ' + plain);
					this.rsa.publicKeyBytes($wnd.base64_decode(publicKey));
					return this.rsa.publicEncrypt(plain);
				}-*/;

				private final native JavaScriptObject privateKeyEncrypt(String privateKey, JsArrayNumber plain) /*-{
				console.log('key: ' + privateKey);
				console.log('decoded key: ' + $wnd.base64_decode(privateKey));
				console.log('encrypting: ' + plain);
					this.rsa.privateKeyBytes($wnd.base64_decode(privateKey));
					return this.rsa.privateEncrypt(plain);
				}-*/;

				private final native JavaScriptObject publicKeyDecrypt(String publicKey, JsArrayNumber encrypted) /*-{
					this.rsa.publicKeyBytes($wnd.base64_decode(publicKey));
					return this.rsa.publicDecrypt(encrypted);
				}-*/;

				private final native JavaScriptObject privateKeyDecrypt(String privateKey, JsArrayNumber encrypted) /*-{
					this.rsa.privateKeyBytes($wnd.base64_decode(privateKey));
					return this.rsa.privateDecrypt(encrypted);
				}-*/;

				private static native RsaJso createRsa(String messageFormat) /*-{
					$wnd.__unit( "RsaJso" );
					$wnd.__uses( "packages.js" );
					$wnd.__uses( "BigInteger.init1.js" );
					$wnd.__uses( "BigInteger.init2.js" );
					$wnd.__uses( "RSA.init1.js" );
					$wnd.__uses( "RSA.init2.js" );
					$wnd.__uses( "RSA.init3.js" );
					$wnd.__uses( "RSAKeyFormat.js" );
					$wnd.__uses( "RSAMessageFormat.js" );
					$wnd.__uses( "RSAMessageFormatSOAEP.js" );
					$wnd.__uses( "RSAMessageFormatBitPadding.js" );
					
					var BigInteger = $wnd.__import( $wnd,"titaniumcore.crypto.BigInteger" );
					var RSA = $wnd.__import( $wnd,"titaniumcore.crypto.RSA" );
					var RSAMessageFormatSOAEP = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatSOAEP" );
					var RSAMessageFormatBitPadding = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatBitPadding" );
					var RSAKeyFormat = $wnd.__import( $wnd, "titaniumcore.crypto.RSAKeyFormat" );
					RSA.installKeyFormat( RSAKeyFormat );
					RSA.installMessageFormat( RSAMessageFormatSOAEP );
					RSA.installMessageFormat( RSAMessageFormatBitPadding );
					
					var rsa = new RSA();
					if (messageFormat == 'SOAEP') {
						console.log('SOAEP');
						rsa.messageFormat = RSAMessageFormatSOAEP;
					} else if (messageFormat == 'BitPadding') {
						rsa.messageFormat = RSAMessageFormatBitPadding;
					}
					return { rsa: rsa };
				}-*/;
			}

			protected AsymmetricCipherImpl(RsaJso rsa) {
				this.rsa = rsa;
			}

			@Override
			public final byte[] encrypt(Key key, byte[] plain) {
				validateKey(key);
				if (key instanceof PublicKey) {
					return encrypt((PublicKey) key, plain);
				} else if (key instanceof PrivateKey) {
					return encrypt((PrivateKey) key, plain);
				}
				throw new IllegalStateException(
						"something went wrong - key: " + key + ", plain: " + Arrays.toString(plain));
			}

			@Override
			public final byte[] decrypt(Key key, byte[] encrypted) {
				validateKey(key);
				if (key instanceof PublicKey) {
					return decrypt((PublicKey) key, encrypted);
				} else if (key instanceof PrivateKey) {
					return decrypt((PrivateKey) key, encrypted);
				}
				throw new IllegalStateException(
						"something went wrong - key: " + key + ", encrypted: " + Arrays.toString(encrypted));
			}

			private void validateKey(Key key) {
				if (!(key instanceof PublicKey || key instanceof PrivateKey)) {
					throw new IllegalArgumentException("key must be instance of " + PublicKey.class + " or "
							+ PrivateKey.class + " but was instance of " + (key == null ? "(null)" : key.getClass()));
				}
			}

			private native final static void log(Object message) /*-{
				console.log(message);
			}-*/;

			@Override
			public final byte[] encrypt(PublicKey publicKey, byte[] plain) {
				log("key: " + publicKey.getEncodedKey());
				log("key: " + Arrays.toString(publicKey.getPlainKey()));
				log("plain: " + Arrays.toString(plain));
				return JsArrayUtils.toByteArray((JsArrayNumber) rsa
						.publicKeyEncrypt(publicKey.getEncodedKey(), JsArrayUtils.toJsArrayNumber(plain)).cast());
			}

			@Override
			public final byte[] encrypt(PrivateKey privateKey, byte[] plain) {
				GWT.log("Private key: " + privateKey);
				GWT.log("Private key: " + privateKey.getEncodedKey());
				GWT.log("Plain array: " + Arrays.toString(plain));
				GWT.log("Plain js array: " + JsArrayUtils.toJsArrayNumber(plain));
				return JsArrayUtils.toByteArray((JsArrayNumber) rsa
						.privateKeyEncrypt(privateKey.getEncodedKey(), JsArrayUtils.toJsArrayNumber(plain)).cast());
			}

			@Override
			public final byte[] decrypt(PublicKey publicKey, byte[] encrypted) {
				return JsArrayUtils.toByteArray((JsArrayNumber) rsa
						.publicKeyDecrypt(publicKey.getEncodedKey(), JsArrayUtils.toJsArrayNumber(encrypted)).cast());
			}

			@Override
			public final byte[] decrypt(PrivateKey privateKey, byte[] encrypted) {
				return JsArrayUtils.toByteArray((JsArrayNumber) rsa
						.privateKeyDecrypt(privateKey.getEncodedKey(), JsArrayUtils.toJsArrayNumber(encrypted)).cast());
			}

		}

		public static AsymmetricCipher createCipher(MessageFormat messageFormat) {
			return new AsymmetricCipherImpl(
					de.kune.phoenix.client.crypto.AsymmetricCipher.Factory.AsymmetricCipherImpl.RsaJso
							.createRsa(messageFormat.name()));
		}

		public static KeyPair createKeyPair(String encodedPublicKey, String encodedPrivateKey) {
			return new KeyPairImpl(new PublicKeyImpl(encodedPublicKey), new PrivateKeyImpl(encodedPrivateKey));
		}
	}

	byte[] encrypt(PublicKey publicKey, byte[] plain);

	byte[] encrypt(PrivateKey privateKey, byte[] plain);

	byte[] decrypt(PublicKey publicKey, byte[] encrypted);

	byte[] decrypt(PrivateKey privateKey, byte[] encrypted);

}
