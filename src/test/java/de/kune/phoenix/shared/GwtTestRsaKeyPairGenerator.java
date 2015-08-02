package de.kune.phoenix.shared;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.shared.RsaKeyPair.KeyType;
import de.kune.phoenix.shared.RsaKeyPair.MessageFormat;
import de.kune.phoenix.shared.RsaKeyPairGenerator.KeyStrength;
import de.kune.phoenix.shared.RsaKeyPairGenerator.PublicExponent;

public class GwtTestRsaKeyPairGenerator extends AsyncGwtTestBase {

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}

	public void testEncryptPublicDecryptPrivateWithGivenKeyPair() {
		delayTestFinish(120000);
		RsaKeyPairGenerator generator = new RsaKeyPairGenerator();
		generator.createAsync(
				"cbVz3B4xbxx2E3l4l0LeCnjD7GkFXyshtxxhtnN5hF8sUVjmdD0zYAQgO7ca2pN4pazU5R7xx1EGr80jZBW5WZH0yA",
				"4wxbdmWP5JYXXdxxfq3fqHa8BunOHCGib0m5dwRkar1J8xajgNeVTVUnZdPzpuAX6xxPUVlbcXLRBuhpHWxaE2bPIs5ZI0tQxxsaRIgukrXEpmw4gPCVmzNSkwfopBm8xx2gCZ3pRDnMEvfXhxbJTQ5xxgxxyfkQ",
				new TestCallback<RsaKeyPair, Exception>() {
					@Override
					public void handleSuccess(RsaKeyPair result) {
						try {
							result.setMessageFormat(MessageFormat.BitPadding);
							assertNotNull(result);
							try {
								final String plainText = "Plain Text";
								GWT.log("plain text: " + plainText);
								byte[] plainBytes = plainText.getBytes("UTF-8");
								GWT.log("plain bytes: " + Arrays.toString(plainBytes));
								byte[] encrypted = result.encrypt(KeyType.PUBLIC, plainBytes);
								GWT.log("Java encrypted: " + Arrays.toString(encrypted));
								byte[] decryptedBytes = result.decrypt(KeyType.PRIVATE, encrypted);
								GWT.log("decrypted bytes: " + decryptedBytes);
								assertEquals(plainText, new String(decryptedBytes, "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								fail("UTF-8 not supported");
							}
						} catch (Throwable e) {
							reportUncaughtException(e);
						}
					}
				});
	}

	public void testEncryptPublicDecryptPrivateWithGeneratedKey() {
		delayTestFinish(120000);
		RsaKeyPairGenerator generator = new RsaKeyPairGenerator();
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

	public void testGenerateRsaKey() {
		delayTestFinish(120000);
		RsaKeyPairGenerator generator = new RsaKeyPairGenerator();
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

}
