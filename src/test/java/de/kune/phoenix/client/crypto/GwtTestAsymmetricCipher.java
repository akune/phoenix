package de.kune.phoenix.client.crypto;

import java.io.UnsupportedEncodingException;

import de.kune.phoenix.client.AsyncGwtTestBase;
import de.kune.phoenix.client.crypto.AsymmetricCipher.MessageFormat;
import de.kune.phoenix.client.crypto.KeyPair.KeyStrength;
import de.kune.phoenix.client.crypto.KeyPair.PublicExponent;

public class GwtTestAsymmetricCipher extends AsyncGwtTestBase {

	@Override
	public String getModuleName() {
		return "de.kune.phoenix.mainjunit";
	}
	
	public void testWithGivenKeyPairBitPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				final AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(MessageFormat.BitPadding);
				final KeyPair keyPair = AsymmetricCipher.Factory.createKeyPair("frH6VoWnODHbgI+1RTEeUD3CVE8FYXwlgHyOV/5QBhFDZ8K6SoYQUvrEHyHzwu0jLLLyUHZQBMP8f5ouxNVxXg==", "EhS6fbossf/H62OOgyICUczUGMXPuoPjnLmVFNPQmYNAWB0V1YPqyZzTBcMaAesyF54ayEc1oCuJzaCn/iCBA6yl3kIzE28e3kbUUXyrENh2jVo8Skyruc1sGk8ZP4+AwvFGrOr33UPLllezHcuRNw==");
				doTest(cipher, keyPair.getPrivateKey(), keyPair.getPublicKey(), "Plain Text");
			}
		});
	}

	public void testWithGivenKeyPairSoaepPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>() {
			protected void handleSuccess(Void nothing) {
				final AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(MessageFormat.SOAEP);
				final KeyPair keyPair = AsymmetricCipher.Factory.createKeyPair("frH6VoWnODHbgI+1RTEeUD3CVE8FYXwlgHyOV/5QBhFDZ8K6SoYQUvrEHyHzwu0jLLLyUHZQBMP8f5ouxNVxXg==", "EhS6fbossf/H62OOgyICUczUGMXPuoPjnLmVFNPQmYNAWB0V1YPqyZzTBcMaAesyF54ayEc1oCuJzaCn/iCBA6yl3kIzE28e3kbUUXyrENh2jVo8Skyruc1sGk8ZP4+AwvFGrOr33UPLllezHcuRNw==");
				doTest(cipher, keyPair.getPrivateKey(), keyPair.getPublicKey(), "Plain Text");
			}
		});
	}


	public void testWithGeneratedKeyPairWeakestKeyStrengthSmallestExponentSoaepPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>(false) {
			@Override
			protected void handleSuccess(Void result) {
				AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(MessageFormat.SOAEP);
				doTest(cipher, KeyStrength.WEAKEST, PublicExponent.SMALLEST);
			}
		});
	}

	public void testWithGeneratedKeyPairWeakestKeyStrengthSmallestExponentBitPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>(false) {
			@Override
			protected void handleSuccess(Void result) {
				AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(MessageFormat.BitPadding);
				doTest(cipher, KeyStrength.WEAKEST, PublicExponent.SMALLEST);
			}
		});
	}
	
	public void testWithGeneratedKeyPairStrongKeyStrengthSmallestExponentSoaepPadding() {
		delayTestFinish(120000);
		CipherSuite.init(new TestCallback<Void, Exception>(false) {
			@Override
			protected void handleSuccess(Void result) {
				AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(MessageFormat.SOAEP);
				doTest(cipher, KeyStrength.STRONG, PublicExponent.SMALLEST);
			}
		});
	}

	private void doTest(final AsymmetricCipher cipher, KeyStrength keyStrength, PublicExponent exponent) {
		AsymmetricCipher.Factory.generateKeyPairAsync(keyStrength, exponent, new TestCallback<KeyPair, Void>() {
			@Override
			public void handleSuccess(KeyPair result) {
				String plainText = "Plain Text";
				doTest(cipher, result.getPublicKey(), result.getPrivateKey(), plainText);
				doTest(cipher, result.getPrivateKey(), result.getPublicKey(), plainText);
			}
		}, new TestCallback<Integer, Void>(false));
	}

	private void doTest(final AsymmetricCipher cipher, Key encryptionKey, Key decryptionKey, String plainText) {
		try {
			byte[] plain = plainText.getBytes("UTF-8");
			byte[] encrypted = cipher.encrypt(encryptionKey, plain);
			assertNotEquals("encrypted must not be equal to plain", plain, encrypted);
			byte[] decrypted = cipher.decrypt(decryptionKey, encrypted);
			assertEquals("decrypted must be equal to plain", plain, decrypted);
		} catch (UnsupportedEncodingException e) {
			fail("UTF-8 not supported");
		}
	}
}
