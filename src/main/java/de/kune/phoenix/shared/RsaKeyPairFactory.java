package de.kune.phoenix.shared;

import static de.kune.phoenix.shared.JsArrayUtil.toByteArray;
import static de.kune.phoenix.shared.JsArrayUtil.toJsArrayNumber;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;

public class RsaKeyPairFactory {

	private State state = State.READY;

	public static enum KeyStrength {
		/**
		 * Weakest key strength of 256 bit.
		 */
		WEAKEST(256), /**
						 * Weak key strength of 512 bit.
						 */
		WEAK(512), /**
					 * Medium key strength of 1024 bit.
					 */
		MEDIUM(1024), /**
						 * Strong key strength of 2048 bit.
						 */
		STRONG(2048), /**
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

	public static enum PublicExponent {
		/**
		 * Smallest public exponent 3.
		 */
		SMALLEST(3), /**
						 * Small public exponent 7.
						 */
		SMALL(7), /**
					 * Medium public exponent 17.
					 */
		MEDIUM(17), /**
					 * Big public exponent 257.
					 */
		BIG(257), /**
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

	private static enum State {
		READY, GENERATING;
	}

	protected static class RsaKeyPairJso extends JavaScriptObject implements RsaKeyPair {
		protected RsaKeyPairJso() {
		}

		public static native RsaKeyPairJso create(String publicKey, String privateKey) /*-{
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
			var result = function(result){};
			var done = function(result) {
				console.log(rsaKey);
				c.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)({rsaKey:rsaKey,privateKey:$wnd.base64x_encode(rsaKey.privateKeyBytes()), publicKey:$wnd.base64x_encode(rsaKey.publicKeyBytes())});
			};
			var rsaKey = new RSA();
			rsaKey.messageFormat = RSAMessageFormatSOAEP;
//			rsaKey.messageFormat = RSAMessageFormatBitPadding;

			return {publicKey: publicKey, privateKey: privateKey, rsaKey: rsaKey};
		}-*/;

		private final native void doSetMessageFormat(String messageFormat) /*-{
			if (messageFormat == 'BitPadding') {
				var RSAMessageFormatBitPadding = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatBitPadding" );
				this.rsaKey.messageFormat = RSAMessageFormatBitPadding;
			} else if (messageFormat == 'SOAEP') {
				var RSAMessageFormatSOAEP = $wnd.__import( $wnd, "titaniumcore.crypto.RSAMessageFormatSOAEP" );
				this.rsaKey.messageFormat = RSAMessageFormatSOAEP;
			}
		}-*/;

		@Override
		public final void setMessageFormat(MessageFormat messageFormat) {
			doSetMessageFormat(messageFormat.name());
		}

		@Override
		public final native String getEncodedPublicKey() /*-{
			return this.publicKey;
		}-*/;

		@Override
		public final native String getEncodedPrivateKey() /*-{
			return this.privateKey;
		}-*/;

		private final native JavaScriptObject publicKeyEncrypt(JsArrayNumber plain) /*-{
			console.log('plain:');
			console.log(plain);
			this.rsaKey.publicKeyBytes($wnd.base64x_decode(this.publicKey));
			var result = this.rsaKey.publicEncrypt(plain);
			console.log('encrypted:');
			console.log(result);
			return result;
		}-*/;

		private final native JavaScriptObject privateKeyEncrypt(JsArrayNumber plain) /*-{
			console.log(plain);
			this.rsaKey.privateKeyBytes($wnd.base64x_decode(this.privateKey));
			var result = this.rsaKey.privateEncrypt(plain);
			console.log(result);
			return result;
		}-*/;

		private final native JavaScriptObject publicKeyDecrypt(JsArrayNumber encrypted) /*-{
			console.log(encrypted);
			this.rsaKey.publicKeyBytes($wnd.base64x_decode(this.publicKey));
			var result = this.rsaKey.publicDecrypt(encrypted);
			console.log(result);
			return result;
		}-*/;

		private final native JavaScriptObject privateKeyDecrypt(JsArrayNumber encrypted) /*-{
			console.log('encrypted:');
			console.log(encrypted);
			this.rsaKey.privateKeyBytes($wnd.base64x_decode(this.privateKey));
			var result = this.rsaKey.privateDecrypt(encrypted);
			console.log('decrypted:');
			console.log(result);
			return result;
		}-*/;

		@Override
		public final byte[] encrypt(KeyType keyType, byte[] plain) {
			switch (keyType) {
			case PRIVATE:
				return toByteArray((JsArrayNumber) privateKeyEncrypt(toJsArrayNumber(plain)).cast());
			case PUBLIC:
				return toByteArray((JsArrayNumber) publicKeyEncrypt(toJsArrayNumber(plain)).cast());
			default:
				throw new IllegalArgumentException("unknown key type " + keyType);
			}
		}

		@Override
		public final byte[] decrypt(KeyType keyType, byte[] encrypted) {
			switch (keyType) {
			case PRIVATE:
				return toByteArray((JsArrayNumber) privateKeyDecrypt(toJsArrayNumber(encrypted)).cast());
			case PUBLIC:
				return toByteArray((JsArrayNumber) publicKeyDecrypt(toJsArrayNumber(encrypted)).cast());
			default:
				throw new IllegalArgumentException("unknown key type " + keyType);
			}
		}

		@Override
		public final int getEncryptMaxSize(KeyType keyType) {
			switch (keyType) {
			case PRIVATE:
				return getPrivateEncryptMaxSize();
			case PUBLIC:
				return getPublicEncryptMaxSize();
			default:
				throw new IllegalArgumentException("unknown key type " + keyType);
			}
		}

		private final native int getPublicEncryptMaxSize() /*-{
			this.rsaKey.publicKeyBytes($wnd.base64x_decode(this.publicKey));
			return this.rsaKey.publicEncryptMaxSize();
		}-*/;

		private final native int getPrivateEncryptMaxSize() /*-{
			this.rsaKey.privateKeyBytes($wnd.base64x_decode(this.privateKey));
			return this.rsaKey.privateEncryptMaxSize();
		}-*/;

	}

	private static final List<String> SCRIPT_FILES = Arrays.asList(new String[] { "{moduleBaseUrl}/all.js" });

	private static boolean scriptsLoaded;

	public RsaKeyPair create(final String encodedPublicKey, final String encodedPrivateKey) {
		assertReady();
		return RsaKeyPairJso.create(encodedPublicKey, encodedPrivateKey);
	}

	public void generateKeyPairAsync(final KeyStrength strength, final PublicExponent exponent,
			final Callback<RsaKeyPair, Exception> doneCallback, final Callback<Integer, Void> progressCallback) {
		assertReady();
		doGenerateKeyPairAsync(strength.getKeySize(), exponent.getExponent(), new Callback<RsaKeyPair, Void>() {
			@Override
			public void onFailure(Void reason) {
				state = State.READY;
				doneCallback.onFailure(new RuntimeException("could not generate key pair"));
			}

			@Override
			public void onSuccess(RsaKeyPair result) {
				state = State.READY;
				doneCallback.onSuccess(result);
			}
		}, progressCallback);
		state = State.GENERATING;
	}

	private void assertReady() {
		CipherSuite.assertReady();
		if (state != State.READY) {
			throw new IllegalArgumentException("key pair factory is not ready");
		}
	}

	private native int doGenerateKeyPairAsync(int keySize, int exponent, Callback<RsaKeyPair, Void> c,
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
		var result = function(result){};
		var done = function(result) {
			console.log(rsaKey);
			c.@com.google.gwt.core.client.Callback::onSuccess(Ljava/lang/Object;)({rsaKey:rsaKey,privateKey:$wnd.base64x_encode(rsaKey.privateKeyBytes()), publicKey:$wnd.base64x_encode(rsaKey.publicKeyBytes())});
		};
		var rsaKey = new RSA();
		rsaKey.messageFormat = RSAMessageFormatSOAEP;
		return rsaKey.generateAsync( keySize, exponent, progress, result, done );
	}-*/;

	// public void init(final Callback<Void, Exception> callback) {
	// if (state == State.UNINITIALIZED) {
	// state = State.INITIALIZING;
	// loadScripts(SCRIPT_FILES, new Callback<Void, Exception>() {
	// @Override
	// public void onFailure(Exception reason) {
	// state = State.UNINITIALIZED;
	// callback.onFailure(new RuntimeException("could not load script files",
	// reason));
	// }
	//
	// @Override
	// public void onSuccess(Void result) {
	// state = State.READY;
	// callback.onSuccess(null);
	// }
	// });
	// } else if (state != State.INITIALIZING) {
	// callback.onSuccess(null);
	// } else {
	// callback.onFailure(new IllegalStateException("initialization in
	// progress"));
	// }
	// }
	//
	// private static void loadScripts(final List<String> scripts, final
	// Callback<Void, Exception> callback) {
	// if (scriptsLoaded || scripts.isEmpty()) {
	// scriptsLoaded = true;
	// callback.onSuccess(null);
	// } else {
	// String script = scripts.iterator().next().replace("{moduleBaseUrl}",
	// GWT.getModuleBaseURL());
	// GWT.log("loading script " + script);
	// ScriptInjector.fromUrl(script).setWindow(TOP_WINDOW).setRemoveTag(false)
	// .setCallback(new Callback<Void, Exception>() {
	// @Override
	// public void onSuccess(Void result) {
	// loadScripts(scripts.subList(1, scripts.size()), callback);
	// }
	//
	// @Override
	// public void onFailure(Exception reason) {
	// callback.onFailure(reason);
	// }
	// }).inject();
	// }
	// }
	//
	// public State getState() {
	// return state;
	// }

}
