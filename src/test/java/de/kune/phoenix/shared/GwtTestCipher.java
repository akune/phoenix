package de.kune.phoenix.shared;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.shared.CipherFactory.Algorithm;
import de.kune.phoenix.shared.CipherFactory.BlockCipherMode;
import de.kune.phoenix.shared.CipherFactory.KeyStrength;
import de.kune.phoenix.shared.CipherFactory.Padding;

public class GwtTestCipher extends AsyncGwtTestBase {

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}

	public void testEncryptionAndDecriptionWithGeneratedCipher() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			@Override
			protected void handleSuccess(Void result) {
				for (Algorithm algorithm : Algorithm.values()) {
					for (KeyStrength keyStrength : KeyStrength.values()) {
						for (BlockCipherMode blockCipherMode : BlockCipherMode.values()) {
							for (Padding padding : Padding.values()) {
								try {
								Cipher cipher = new CipherFactory().generate(algorithm, keyStrength, blockCipherMode,
										padding);
								doTestCipher(cipher);
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
				Cipher cipher = new CipherFactory().create(Algorithm.RIJNDAEL,
						"AupyROrgqnkRZRiHGQXTkdoTcWj+8W1NBkfd311kmFk=", BlockCipherMode.ECB, Padding.PKCS7);
				doTestCipher(cipher);
			}
		});
	}

	private void doTestCipher(Cipher cipher) {
		try {
			String plainString = "Plain Text";
			GWT.log("plain text: " + plainString);
			byte[] plainBytes = plainString.getBytes("UTF-8");
			GWT.log("plain bytes: " + Arrays.toString(plainBytes));
			byte[] encryptedBytes = cipher.encrypt(plainBytes);
			GWT.log("encrypted bytes: " + Arrays.toString(encryptedBytes));
			byte[] decryptedBytes = cipher.decrypt(encryptedBytes);
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
