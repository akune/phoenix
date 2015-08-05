package de.kune.phoenix.shared;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.shared.GWT;

import de.kune.phoenix.shared.RsaKeyPair.KeyType;
import de.kune.phoenix.shared.RsaKeyPair.MessageFormat;

public class GwtTestRsaKeyPair extends AsyncGwtTestBase {

	private static final String PRIVATE_KEY = "4wxbdmWP5JYXXdxxfq3fqHa8BunOHCGib0m5dwRkar1J8xajgNeVTVUnZdPzpuAX6xxPUVlbcXLRBuhpHWxaE2bPIs5ZI0tQxxsaRIgukrXEpmw4gPCVmzNSkwfopBm8xx2gCZ3pRDnMEvfXhxbJTQ5xxgxxyfkQ";
	private static final String PUBLIC_KEY = "cbVz3B4xbxx2E3l4l0LeCnjD7GkFXyshtxxhtnN5hF8sUVjmdD0zYAQgO7ca2pN4pazU5R7xx1EGr80jZBW5WZH0yA";

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}

	public void testEncryptPublicDecryptPrivateWithGivenKeyPairBitPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				RsaKeyPairFactory generator = new RsaKeyPairFactory();
				RsaKeyPair result = generator.create(PUBLIC_KEY, PRIVATE_KEY);
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
			}
		});
	}

	public void testEncryptPublicDecryptPrivateWithGivenKeyPairSoaepPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				RsaKeyPairFactory generator = new RsaKeyPairFactory();
				RsaKeyPair result = generator.create(PUBLIC_KEY, PRIVATE_KEY);
				result.setMessageFormat(MessageFormat.SOAEP);
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
			}
		});
	}

	public void testEncryptPrivateDecryptPublicWithGivenKeyPairSoaepPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				RsaKeyPairFactory generator = new RsaKeyPairFactory();
				RsaKeyPair result = generator.create(PUBLIC_KEY, PRIVATE_KEY);
				result.setMessageFormat(MessageFormat.SOAEP);
				assertNotNull(result);
				try {
					final String plainText = "Plain Text";
					GWT.log("plain text: " + plainText);
					byte[] plainBytes = plainText.getBytes("UTF-8");
					GWT.log("plain bytes: " + Arrays.toString(plainBytes));
					byte[] encrypted = result.encrypt(KeyType.PRIVATE, plainBytes);
					GWT.log("Java encrypted: " + Arrays.toString(encrypted));
					byte[] decryptedBytes = result.decrypt(KeyType.PUBLIC, encrypted);
					GWT.log("decrypted bytes: " + decryptedBytes);
					assertEquals(plainText, new String(decryptedBytes, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					fail("UTF-8 not supported");
				}
			}
		});
	}

	public void testEncryptPrivateDecryptPublicWithGivenKeyPairBitPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				RsaKeyPairFactory generator = new RsaKeyPairFactory();
				RsaKeyPair result = generator.create(PUBLIC_KEY, PRIVATE_KEY);
				result.setMessageFormat(MessageFormat.BitPadding);
				assertNotNull(result);
				try {
					final String plainText = "Plain Text";
					GWT.log("plain text: " + plainText);
					byte[] plainBytes = plainText.getBytes("UTF-8");
					GWT.log("plain bytes: " + Arrays.toString(plainBytes));
					byte[] encrypted = result.encrypt(KeyType.PRIVATE, plainBytes);
					GWT.log("Java encrypted: " + Arrays.toString(encrypted));
					byte[] decryptedBytes = result.decrypt(KeyType.PUBLIC, encrypted);
					GWT.log("decrypted bytes: " + decryptedBytes);
					assertEquals(plainText, new String(decryptedBytes, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					fail("UTF-8 not supported");
				}
			}
		});
	}

}
