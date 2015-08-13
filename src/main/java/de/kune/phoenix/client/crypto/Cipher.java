package de.kune.phoenix.client.crypto;

import com.google.gwt.core.client.JsArrayNumber;

import de.kune.phoenix.client.crypto.util.Base64Utils;
import de.kune.phoenix.client.crypto.util.JsArrayUtils;

public interface Cipher {

	abstract class EncodedKey implements Key {
		private final String encodedKey;
		public EncodedKey(String encodedKey) {
			this.encodedKey = encodedKey;
		}
		@Override
		public String getEncodedKey() {
			return encodedKey;
		}
		@Override
		public byte[] getPlainKey() {
			return Base64Utils.decode(encodedKey);
		}
	}
	
	abstract class JsArrayNumberKey implements Key {
		
		private final JsArrayNumber key;

		protected JsArrayNumberKey(JsArrayNumber key) {
			this.key = key;
		}

		@Override
		public final String getEncodedKey() {
			return Base64Utils.encode(getPlainKey());
		}

		@Override
		public final byte[] getPlainKey() {
			return JsArrayUtils.toByteArray(getKey());
		}

		private final JsArrayNumber getKey() {
			return this.key;
		}

	}
	
	byte[] encrypt(Key key, byte[] plain);
	byte[] decrypt(Key key, byte[] encrypted);
	
}
