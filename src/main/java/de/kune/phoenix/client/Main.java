package de.kune.phoenix.client;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import de.kune.phoenix.shared.Cipher;
import de.kune.phoenix.shared.CipherFactory;
import de.kune.phoenix.shared.CipherFactory.Algorithm;
import de.kune.phoenix.shared.CipherFactory.BlockCipherMode;
import de.kune.phoenix.shared.CipherFactory.Padding;
import de.kune.phoenix.shared.CipherSuite;
import de.kune.phoenix.shared.RsaKeyPair;
import de.kune.phoenix.shared.RsaKeyPairFactory;
import de.kune.phoenix.shared.RsaKeyPair.MessageFormat;
import de.kune.phoenix.shared.RsaKeyPairFactory.KeyStrength;
import de.kune.phoenix.shared.RsaKeyPairFactory.PublicExponent;

public class Main implements EntryPoint {

	public void onModuleLoad() {
		final Panel panel = new VerticalPanel();
		final Label welcomeLabel = new Label("Generating key pair");
		panel.add(welcomeLabel);
		RootPanel.get().add(panel);
		GWT.log("initializing cipher suite");
		CipherSuite.init(new Callback<Void, Exception>() {
			@Override
			public void onFailure(Exception reason) {}
			@Override
			public void onSuccess(Void result) {
//				new CipherFactory().generate(Algorithm.RIJNDAEL, CipherFactory.KeyStrength.STRONGEST, BlockCipherMode.ECB,
//						Padding.PKCS7);

				
				Cipher cipher = new CipherFactory().create(Algorithm.RIJNDAEL,
						"AupyROrgqnkRZRiHGQXTkdoTcWj+8W1NBkfd311kmFk=", BlockCipherMode.ECB, Padding.PKCS7);
				GWT.log("decrypted: " + new String(cipher.decrypt(cipher.encrypt("Hello".getBytes()))));
				
				RsaKeyPairFactory factory = new RsaKeyPairFactory();
				RsaKeyPair keyPair = factory.create(
						"cbVz3B4xbxx2E3l4l0LeCnjD7GkFXyshtxxhtnN5hF8sUVjmdD0zYAQgO7ca2pN4pazU5R7xx1EGr80jZBW5WZH0yA",
						"4wxbdmWP5JYXXdxxfq3fqHa8BunOHCGib0m5dwRkar1J8xajgNeVTVUnZdPzpuAX6xxPUVlbcXLRBuhpHWxaE2bPIs5ZI0tQxxsaRIgukrXEpmw4gPCVmzNSkwfopBm8xx2gCZ3pRDnMEvfXhxbJTQ5xxgxxyfkQ");
				try {
					keyPair.setMessageFormat(MessageFormat.BitPadding);
					byte[] encrypted = keyPair.encrypt(RsaKeyPair.KeyType.PUBLIC, "0123456789abcde".getBytes("UTF-8"));
					GWT.log("Encrypted: " + Arrays.toString(encrypted));
					GWT.log(new String(keyPair.decrypt(RsaKeyPair.KeyType.PRIVATE, encrypted), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				factory.generateKeyPairAsync(KeyStrength.MEDIUM, PublicExponent.SMALLEST,
						new Callback<RsaKeyPair, Exception>() {
					@Override
					public void onSuccess(RsaKeyPair result) {
						welcomeLabel.setText(welcomeLabel.getText() + "done");
						panel.add(new Anchor(
								"Use generated key pair for encryption / decryption", "titaniumcore/encryption.html?pu="
										+ result.getEncodedPublicKey() + "&pr=" + result.getEncodedPrivateKey() + "",
								"_blank"));
						GWT.log("private key=" + result.getEncodedPrivateKey());
						GWT.log("public key=" + result.getEncodedPublicKey());
						GWT.log("max private key encrypt size=" + result.getEncryptMaxSize(RsaKeyPair.KeyType.PRIVATE));
						GWT.log("max public key encrypt size=" + result.getEncryptMaxSize(RsaKeyPair.KeyType.PUBLIC));
						try {
							byte[] encrypted = result.encrypt(RsaKeyPair.KeyType.PUBLIC,
									"Plain Text".getBytes("UTF-8"));
							GWT.log("Encrypted: " + Arrays.toString(encrypted));
							GWT.log(new String(result.decrypt(RsaKeyPair.KeyType.PRIVATE, encrypted), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onFailure(Exception reason) {
						welcomeLabel.setText(welcomeLabel.getText() + "failed");
					}
				}, new Callback<Integer, Void>() {
					@Override
					public void onFailure(Void reason) {
					}

					@Override
					public void onSuccess(Integer progress) {
						// GWT.log("progressing, state=" +
						// generator.getState());
						if (progress % 500 == 0) {
							welcomeLabel.setText(welcomeLabel.getText() + ".");
						}
					}
				});
			}
		});

	}

}
