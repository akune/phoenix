package de.kune.phoenix.client.crypto;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.client.AsyncGwtTestBase;
import de.kune.phoenix.client.crypto.SymmetricCipher.Algorithm;
import de.kune.phoenix.client.crypto.SymmetricCipher.BlockCipherMode;
import de.kune.phoenix.client.crypto.SymmetricCipher.Padding;
import de.kune.phoenix.client.crypto.SecretKey.KeyStrength;

public class GwtTestSymmetricCipher extends AsyncGwtTestBase {

	private static final String ENCODED_SECRET_KEY = "AupyROrgqnkRZRiHGQXTkdoTcWj+8W1NBkfd311kmFk=";

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}

	public void testEncryptionAndDecriptionWithGeneratedCipher() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			@Override
			protected void handleSuccess(Void result) {
				for (KeyStrength keyStrength : KeyStrength.values()) {
					SecretKey secretKey = SymmetricCipher.Factory.generateSecretKey(keyStrength);
					for (Algorithm algorithm : Algorithm.values()) {
						for (BlockCipherMode blockCipherMode : BlockCipherMode.values()) {
							for (Padding padding : Padding.values()) {
								try {
									SymmetricCipher cipher = SymmetricCipher.Factory.createCipher(algorithm,
											blockCipherMode, padding);
									doTestCipher(cipher, secretKey);
								} catch (Throwable t) {
									fail("algorithm=" + algorithm + ", key strength=" + keyStrength
											+ ", block cipher mode=" + blockCipherMode + ", padding=" + padding
											+ " failure message=[" + t.getMessage() + "]");
								}
							}
						}
					}
				}
			}
		});
	}

	public void testRijndaelEncryptionAndDecryption() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			@Override
			protected void handleSuccess(Void result) {
				SecretKey secretKey = SymmetricCipher.Factory.createSecretKey(ENCODED_SECRET_KEY);
				SymmetricCipher cipher = SymmetricCipher.Factory.createCipher(Algorithm.RIJNDAEL, BlockCipherMode.ECB,
						Padding.PKCS7);
				doTestCipher(cipher, secretKey);
			}
		});
	}

	private void doTestCipher(SymmetricCipher cipher, SecretKey secretKey) {
		try {
			String plainString = "Plain Text";
			GWT.log("plain text: " + plainString);
			byte[] plainBytes = plainString.getBytes("UTF-8");
			GWT.log("plain bytes: " + Arrays.toString(plainBytes));
			byte[] encryptedBytes = cipher.encrypt(secretKey, plainBytes);
			GWT.log("encrypted bytes: " + Arrays.toString(encryptedBytes));
			byte[] decryptedBytes = cipher.decrypt(secretKey, encryptedBytes);
			GWT.log("decrypted bytes: " + Arrays.toString(decryptedBytes));
			assertTrue("expected " + Arrays.toString(plainBytes) + " but was " + Arrays.toString(decryptedBytes),
					Arrays.equals(plainBytes, decryptedBytes));
			String decryptedString = new String(decryptedBytes, "UTF-8");
			assertEquals(plainString, decryptedString);
		} catch (UnsupportedEncodingException e) {
			fail("UTF-8 not supported");
		}
	}

}
