package de.kune.phoenix.client;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.CipherSuite;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.KeyPair.PublicExponent;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.crypto.SymmetricCipher;
import de.kune.phoenix.client.crypto.SymmetricCipher.Algorithm;
import de.kune.phoenix.client.crypto.SymmetricCipher.BlockCipherMode;
import de.kune.phoenix.client.crypto.SymmetricCipher.Padding;

public class Main implements EntryPoint {

	private Panel chatPanel;
	private Panel mainPanel;
	private Panel avatarPanel;
	private Panel speakPanel;

	private static enum Position {
		LEFT, RIGHT
	}

	protected Widget speak(Widget widget, Position position) {
		final Panel panel = new SimplePanel(widget);
		panel.setStyleName("bubble" + (position == Position.RIGHT ? " bubble--alt right" : ""));
		this.chatPanel.add(panel);
		widget.getElement().scrollIntoView();
		return widget;
	}

	protected Label speak(String what, Position position) {
		final Label label = new Label(what);
		label.setStyleName("bubble" + (position == Position.RIGHT ? " bubble--alt right" : ""));
		chatPanel.add(label);
		label.getElement().scrollIntoView();
		return label;
	}

	public void onModuleLoad() {
		mainPanel = new FlowPanel();
		mainPanel.setStyleName("phoenix container");
		RootPanel.get().add(mainPanel);
		avatarPanel = new FlowPanel();
		avatarPanel.setStyleName("avatar-panel");
		mainPanel.add(avatarPanel);
		chatPanel = new FlowPanel();
		chatPanel.setStyleName("chat-panel");
		mainPanel.add(chatPanel);
		speakPanel = new FlowPanel();
		speakPanel.setStyleName("speak-panel");
		mainPanel.add(speakPanel);

		Image phoenixAvatar = new Image("img/phoenix-avatar.png");
		phoenixAvatar.setWidth("128px");
		avatarPanel.add(phoenixAvatar);

		TextBox speakBox = new TextBox();
		speakPanel.add(speakBox);
		speakBox.addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				// TODO Auto-generated method stub
				// speak("Key stroke: " + event.getNativeKeyCode() ==
				// KeyCodes.KEY_ENTER, Position.RIGHT);
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					speak(((TextBox) event.getSource()).getText(), Position.RIGHT);
					((TextBox) event.getSource()).setText("");
					// TODO: Encrypt with session key and send
				}
			}
		});

		speak("Welcome to Phoenix!", Position.LEFT);
		speak("What is Phoenix?", Position.RIGHT);
		final Label initializingLabel = speak("Initializing...", Position.LEFT);
		GWT.log("initializing cipher suite");
		CipherSuite.init(new Callback<Void, Exception>() {
			@Override
			public void onFailure(Exception reason) {
			}

			@Override
			public void onSuccess(Void result) {
				initializingLabel.setText(initializingLabel.getText() + "done");

				SecretKey secretKey = SymmetricCipher.Factory
						.createSecretKey("AupyROrgqnkRZRiHGQXTkdoTcWj+8W1NBkfd311kmFk=");
				SymmetricCipher cipher = SymmetricCipher.Factory.createCipher(Algorithm.RIJNDAEL, BlockCipherMode.ECB,
						Padding.PKCS7);
				GWT.log("decrypted: "
						+ new String(cipher.decrypt(secretKey, cipher.encrypt(secretKey, "Hello".getBytes()))));

				final Label keyPairMessage = speak("Generating key pair", Position.LEFT);
				AsymmetricCipher.Factory.generateKeyPairAsync(KeyPair.KeyStrength.WEAKEST, PublicExponent.SMALLEST,
						new Callback<KeyPair, Void>() {

					@Override
					public void onFailure(Void reason) {
						keyPairMessage.setText(keyPairMessage.getText() + "failed");
					}

					@Override
					public void onSuccess(KeyPair result) {
						keyPairMessage.setText(keyPairMessage.getText() + "done");
						speak("Public key: " + result.getPublicKey().getEncodedKey(), Position.LEFT);
						speak("Private key: " + result.getPrivateKey().getEncodedKey(), Position.LEFT);
						AsymmetricCipher cipher = AsymmetricCipher.Factory.createCipher(AsymmetricCipher.MessageFormat.SOAEP);
						try {
							byte[] encrypted = cipher.encrypt(result.getPublicKey(), "Plain Text".getBytes("UTF-8"));
							speak("Encrypted: " + Arrays.toString(encrypted), Position.LEFT);
							speak("Decrypted: " + new String(cipher.decrypt(result.getPrivateKey(), encrypted)), Position.LEFT);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}

				}, new Callback<Integer, Void>() {

					@Override
					public void onFailure(Void reason) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onSuccess(Integer progress) {
						if (progress % 500 == 0) {
							keyPairMessage.setText(keyPairMessage.getText() + ".");
						}
					}
				});

				// factory.generateKeyPairAsync(KeyStrength.MEDIUM,
				// PublicExponent.SMALLEST,
				// new Callback<RsaKeyPair, Exception>() {
				// @Override
				// public void onSuccess(RsaKeyPair result) {
				// welcomeLabel.setText(welcomeLabel.getText() + "done");
				// speak(new Anchor(
				// "Use generated key pair for encryption / decryption",
				// "titaniumcore/encryption.html?pu="
				// + result.getEncodedPublicKey() + "&pr=" +
				// result.getEncodedPrivateKey() + "",
				// "_blank"), Position.LEFT);
				// GWT.log("private key=" + result.getEncodedPrivateKey());
				// GWT.log("public key=" + result.getEncodedPublicKey());
				// GWT.log("max private key encrypt size=" +
				// result.getEncryptMaxSize(RsaKeyPair.KeyType.PRIVATE));
				// GWT.log("max public key encrypt size=" +
				// result.getEncryptMaxSize(RsaKeyPair.KeyType.PUBLIC));
				// speak("Encrypting \"Plain Text\" with generated public key.",
				// Position.LEFT);
				// try {
				// byte[] encrypted = result.encrypt(RsaKeyPair.KeyType.PUBLIC,
				// "Plain Text".getBytes("UTF-8"));
				// speak("Encrypted result: " + Arrays.toString(encrypted),
				// Position.LEFT);
				// GWT.log("Encrypted: " + Arrays.toString(encrypted));
				// speak("Decrypting with generated private key.",
				// Position.LEFT);
				// GWT.log(new String(result.decrypt(RsaKeyPair.KeyType.PRIVATE,
				// encrypted), "UTF-8"));
				// speak("Decrypted result: "
				// + new String(result.decrypt(RsaKeyPair.KeyType.PRIVATE,
				// encrypted), "UTF-8"),
				// Position.LEFT);
				//
				// } catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
				// }
				//
				// speak("Generating AES session key.", Position.LEFT);
				// SecretKey aesKey =
				// SymmetricCipher.Factory.generateSecretKey(SecretKey.KeyStrength.STRONGEST);
				// SymmetricCipher aesCipher =
				// SymmetricCipher.Factory.createCipher(Algorithm.RIJNDAEL,
				// BlockCipherMode.ECB,
				// Padding.PKCS7);
				// speak("Encrypting \"Plain Text\" with generated AES key.",
				// Position.LEFT);
				// try {
				// byte[] encrypted = aesCipher.encrypt(aesKey, "Plain
				// Text".getBytes("UTF-8"));
				// speak("Encrypted result: " + Arrays.toString(encrypted),
				// Position.LEFT);
				// speak("Decrypting with generated AES key.", Position.LEFT);
				// speak("Decrypted result: " + new
				// String(aesCipher.decrypt(aesKey, encrypted), "UTF-8"),
				// Position.LEFT);
				// } catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
				// }
				//
				// }
				//
				// @Override
				// public void onFailure(Exception reason) {
				// welcomeLabel.setText(welcomeLabel.getText() + "failed");
				// }
				// }, new Callback<Integer, Void>() {
				// @Override
				// public void onFailure(Void reason) {
				// }
				//
				// @Override
				// public void onSuccess(Integer progress) {
				// // GWT.log("progressing, state=" +
				// // generator.getState());
				// if (progress % 500 == 0) {
				// welcomeLabel.setText(welcomeLabel.getText() + ".");
				// }
				// }
				// });
			}
		});

	}

	// protected ConversationService createConversationsService() {
	// return new ConversationService() {
	//
	// private List<Conversation> conversations = new ArrayList<Conversation>();
	// private RsaKeyPair keyPair;
	//
	// {
	// conversations.add(new Conversation() {
	//
	// private List<Participant> participants = new
	// ArrayList<ConversationService.Participant>();
	// private List<Message> messages = new
	// ArrayList<ConversationService.Message>();
	// private Map<String, byte[]> encryptedKeysById = new HashMap<String,
	// byte[]>();
	//
	// {
	// participants.add(new Participant() {
	//
	// @Override
	// public String getScreenName() {
	// return "Test Participant";
	// }
	//
	// @Override
	// public byte[] getPublicKey() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public String getId() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	// });
	//
	// messages.add(new Message() {
	//
	// private String keyId = "testKeyId";
	// private byte[] encryptedContent;
	//
	// @Override
	// public Date getTimestamp() {
	// return new Date();
	// }
	//
	// @Override
	// public Participant getSender() {
	// return getParticipants().iterator().next();
	// }
	//
	// @Override
	// public String getContent() {
	// try {
	// return new String(getCipherById(keyId).decrypt(encryptedContent),
	// "UTF-8");
	// } catch (UnsupportedEncodingException e) {
	// return e.getMessage();
	// }
	// }
	// });
	// }
	//
	// protected Cipher getCipherById(String keyId) {
	// byte[] decryptedKey = keyPair.decrypt(KeyType.PRIVATE,
	// encryptedKeysById.get(keyId));
	// return new CipherFactory().create(Algorithm.RIJNDAEL, decryptedKey,
	// BlockCipherMode.ECB,
	// Padding.PKCS7);
	// }
	//
	// @Override
	// public void sendMessage(String message) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public List<Message> receiveNewMessages() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public List<Participant> getParticipants() {
	// return participants;
	// }
	//
	// @Override
	// public String getId() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public List<Message> getAllMessages() {
	// return messages;
	// }
	// });
	// }
	//
	// @Override
	// public void login(RsaKeyPair keyPair) {
	// this.keyPair = keyPair;
	// }
	//
	// @Override
	// public List<Conversation> getConversations() {
	// return conversations;
	// }
	// };
	// }

}
