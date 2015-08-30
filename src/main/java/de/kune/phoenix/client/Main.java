package de.kune.phoenix.client;

import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;

import org.fusesource.restygwt.client.Defaults;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
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
import de.kune.phoenix.client.crypto.Key;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.KeyPair.PublicExponent;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.messaging.ClientSession;
import de.kune.phoenix.client.messaging.ConversationSession;
import de.kune.phoenix.client.messaging.InvitationCallback;
import de.kune.phoenix.client.messaging.MessageCallback;
import de.kune.phoenix.shared.Message;

public class Main implements EntryPoint {

	private ConversationSession conversationSession;

	private Panel chatPanel;
	private Panel mainPanel;
	private Panel avatarPanel;
	private Panel speakPanel;

	private ClientSession clientSession;

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

	private Panel speak(Key key, String label, Position position) {
		FlowPanel keyPanel = new FlowPanel();
		keyPanel.add(new Label(label));
		Anchor keyAnchor = new Anchor(key.getId());
		keyPanel.add(keyAnchor);
		final Label keyLabel = new Label(key.getEncodedKey());
		keyLabel.setVisible(false);
		keyPanel.add(keyLabel);
		keyAnchor.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				GWT.log("doh!");
				keyLabel.setVisible(!keyLabel.isVisible());
			}
		});
		speak(keyPanel, position);
		return keyPanel;
	}

	protected Label speak(String what, Position position) {
		final Label label = new Label(what);
		label.setStyleName("bubble" + (position == Position.RIGHT ? " bubble--alt right" : ""));
		chatPanel.add(label);
		label.getElement().scrollIntoView();
		return label;
	}

	public void onModuleLoad() {
		Defaults.setServiceRoot(com.google.gwt.core.client.GWT.getModuleBaseURL()
				.replace(com.google.gwt.core.client.GWT.getModuleName() + "/", "") + "api");
		Defaults.setDateFormat(null);

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
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					String text = ((TextBox) event.getSource()).getText();
//					speak(text, Position.RIGHT);
					((TextBox) event.getSource()).setText("");
					conversationSession.send(text);
				}
			}
		});

		speak("Welcome to Phoenix!", Position.LEFT);
		final Label initializingLabel = speak("Initializing Cipher Suite...", Position.LEFT);
		CipherSuite.init(new Callback<Void, Exception>() {
			@Override
			public void onFailure(Exception reason) {
			}

			@Override
			public void onSuccess(Void result) {
				initializingLabel.setText(initializingLabel.getText() + "done");

				if (Window.Location.getParameter("pub") != null && Window.Location.getParameter("priv") != null) {
					speak("Key pair found in request.", Position.LEFT);
					final KeyPair keyPair = AsymmetricCipher.Factory.createKeyPair(
							URL.decodePathSegment(Window.Location.getParameter("pub")),
							URL.decodePathSegment(Window.Location.getParameter("priv")));
					speak(keyPair.getPublicKey(), "Public key: ", Position.LEFT);
					speak(keyPair.getPrivateKey(), "Private key: ", Position.LEFT);
					beginClientSession(keyPair);
				} else {
					final Label keyPairMessage = speak("Generating key pair...", Position.LEFT);
					AsymmetricCipher.Factory.generateKeyPairAsync(KeyPair.KeyStrength.MEDIUM, PublicExponent.SMALLEST,
							new Callback<KeyPair, Void>() {

						@Override
						public void onFailure(Void reason) {
							keyPairMessage.setText(keyPairMessage.getText() + "failed");
						}

						@Override
						public void onSuccess(KeyPair result) {
							keyPairMessage.setText(keyPairMessage.getText() + "done");
							Anchor link = new Anchor("Use your new key pair");
							link.setHref(com.google.gwt.core.client.GWT.getHostPageBaseURL() + "?priv="
									+ URL.encodePathSegment(result.getPrivateKey().getEncodedKey()) + "&pub="
									+ URL.encodePathSegment(result.getPublicKey().getEncodedKey()));
							speak(link, Position.LEFT);
							speak(result.getPublicKey(), "Public key: ", Position.LEFT);
							speak(result.getPrivateKey(), "Private key: ", Position.LEFT);
							beginClientSession(result);
						}

					}, new Callback<Integer, Void>() {
						@Override
						public void onFailure(Void reason) {
							throw new RuntimeException();
						}

						@Override
						public void onSuccess(Integer progress) {
							if (progress % 500 == 0) {
								keyPairMessage.setText(keyPairMessage.getText() + ".");
							}
						}
					});
				}

			}
		});
	}

	private void beginClientSession(final KeyPair keyPair) {
		clientSession = new ClientSession(keyPair, asList(keyPair.getPublicKey()));

		speak(createInvitePanel(keyPair), Position.LEFT);
		// speak(importPublicKeyPanel(), Position.LEFT);

		GWT.log("starting client session");
		clientSession.start(new InvitationCallback() {
			@Override
			public void handleInvitation(final ConversationSession conversation) {
				GWT.log("Got invited to " + conversation.getId());
				Main.this.conversationSession = conversation;
				conversation.start(new MessageCallback() {

					@Override
					public void handleReceivedMessage(Message message, byte[] content) {
						try {
							String messageString = new String(content, "UTF-8");
							if (conversation.isSender(message.getSenderId())) {
								speak(messageString, Position.RIGHT);
							} else {
								speak(messageString, Position.LEFT);
							}
						} catch (UnsupportedEncodingException e) {
							throw new IllegalStateException(e);
						}
					}
				});
			}
		});
	}

	private FlowPanel createInvitePanel(final KeyPair keyPair) {
		FlowPanel invitePanel = new FlowPanel();
		invitePanel.add(new Label("Invite: "));
		final TextBox publicKeyTextBox = new TextBox();
		publicKeyTextBox.getElement().setAttribute("placeholder", "Public Key");
		invitePanel.add(publicKeyTextBox);
		publicKeyTextBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !publicKeyTextBox.getText().isEmpty()) {
					PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(publicKeyTextBox.getText());
					GWT.log("Inviting " + publicKey.getId());
					conversationSession = clientSession.beginConversation(asList(keyPair.getPublicKey().getId()));
					conversationSession.invite(publicKey);
				}
			}
		});
		return invitePanel;
	}

	// private FlowPanel importPublicKeyPanel() {
	// FlowPanel invitePanel = new FlowPanel();
	// invitePanel.add(new Label("Import: "));
	// final TextBox publicKeyTextBox = new TextBox();
	// publicKeyTextBox.getElement().setAttribute("placeholder", "Public Key");
	// invitePanel.add(publicKeyTextBox);
	// publicKeyTextBox.addKeyUpHandler(new KeyUpHandler() {
	// @Override
	// public void onKeyUp(KeyUpEvent event) {
	// if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER &&
	// !publicKeyTextBox.getText().isEmpty()) {
	// PublicKey publicKey =
	// AsymmetricCipher.Factory.createPublicKey(publicKeyTextBox.getText());
	// GWT.log("Importing " + publicKey.getId());
	// clientSession.importPublicKey(publicKey);
	// }
	// }
	// });
	// return invitePanel;
	// }

}
