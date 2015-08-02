package de.kune.phoenix.shared;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.shared.RsaKeyPair.KeyType;
import de.kune.phoenix.shared.RsaKeyPairFactory.KeyStrength;
import de.kune.phoenix.shared.RsaKeyPairFactory.PublicExponent;

public class GwtTestRsaKeyPairFactory extends AsyncGwtTestBase {

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}

	public void testEncryptPublicDecryptPrivateWithGeneratedKey() {
		delayTestFinish(120000);
		RsaKeyPairFactory generator = new RsaKeyPairFactory();
		generator.generateKeyPairAsync(KeyStrength.WEAK, PublicExponent.SMALLEST,
				new TestCallback<RsaKeyPair, Exception>() {
					@Override
					public void handleSuccess(RsaKeyPair result) {
						try {
							assertNotNull(result);
							try {
								final String plainText = "Plain Text";
								GWT.log("plain text: " + plainText);
								byte[] plainBytes = plainText.getBytes("UTF-8");
								GWT.log("plain bytes: " + Arrays.toString(plainBytes));
								byte[] encrypted = result.encrypt(KeyType.PUBLIC, plainBytes);
								GWT.log("Java encrypted: " + Arrays.toString(encrypted));
								byte[] decryptedBytes = result.decrypt(KeyType.PRIVATE, encrypted);
								GWT.log("decrypted bytes: " + Arrays.toString(decryptedBytes));
								assertEquals(plainText, new String(decryptedBytes, "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								fail("UTF-8 not supported");
							}
						} catch (Throwable e) {
							reportUncaughtException(e);
						}
					}
				}, new TestCallback<Integer, Void>(false));
	}

	public void testGenerateWeakRsaKey() {
		delayTestFinish(120000);
		RsaKeyPairFactory generator = new RsaKeyPairFactory();
		generator.generateKeyPairAsync(KeyStrength.WEAK, PublicExponent.SMALLEST,
				new TestCallback<RsaKeyPair, Exception>() {
					@Override
					public void handleSuccess(RsaKeyPair result) {
						assertSame("invalid public key encrypt max size", 47, result.getEncryptMaxSize(KeyType.PUBLIC));
						assertSame("invalid private key encrypt max size", 47,
								result.getEncryptMaxSize(KeyType.PRIVATE));
						assertNotNull(result);
						finishTest();
					}
				}, new TestCallback<Integer, Void>(false));
	}

	public void testGenerateMediumRsaKey() {
		delayTestFinish(120000);
		RsaKeyPairFactory generator = new RsaKeyPairFactory();
		generator.generateKeyPairAsync(KeyStrength.MEDIUM, PublicExponent.SMALLEST,
				new TestCallback<RsaKeyPair, Exception>() {
			@Override
			public void handleSuccess(RsaKeyPair result) {
				assertSame("invalid public key encrypt max size", 111, result.getEncryptMaxSize(KeyType.PUBLIC));
				assertSame("invalid private key encrypt max size", 111,
						result.getEncryptMaxSize(KeyType.PRIVATE));
				assertNotNull(result);
				finishTest();
			}
		}, new TestCallback<Integer, Void>(false));
	}
	
}
