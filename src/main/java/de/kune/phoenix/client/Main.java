package de.kune.phoenix.client;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
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
import de.kune.phoenix.client.messaging.Conversation;
import de.kune.phoenix.shared.Message;

public class Main implements EntryPoint {

	// private ConversationSession selectedConversationSession;

	private Panel mainPanel;
	private Panel avatarPanel;
	private Panel speakPanel;

	private ClientSession clientSession;

	private static enum Position {
		LEFT, RIGHT
	}

	protected Widget speak(Panel container, Widget widget, Position position) {
		final Panel panel = new SimplePanel(widget);
		panel.setStyleName("bubble" + (position == Position.RIGHT ? " bubble--alt right" : ""));
		container.add(panel);
		widget.getElement().scrollIntoView();
		return widget;
	}

	private Panel speak(Panel container, Key key, String label, Position position) {
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
				keyLabel.setVisible(!keyLabel.isVisible());
			}
		});
		speak(container, keyPanel, position);
		return keyPanel;
	}

	protected Label speak(Panel container, String what, Position position) {
		final Label label = new Label(what);
		label.setStyleName("bubble" + (position == Position.RIGHT ? " bubble--alt right" : ""));
		container.add(label);
		label.getElement().scrollIntoView();
		return label;
	}

	private void handleNewConversation(Conversation.Builder builder) {
		builder.receivedMessageHandler((m, c) -> handleReceivedMessage(builder.getConversationId(), m, c));
	}

	private Label handleReceivedMessage(String conversationId, Message m, byte[] c) {
		Conversation conversation = clientSession.getConversation(conversationId);
		getConversationAvatarPanel(conversationId);
		Panel panel = getChatPanel(conversationId);
		if (conversation.getKeyPair().getPublicKey().getId().equals(m.getSenderId())) {
			return speak(panel, new String(c), Position.RIGHT);
		} else {
			return speak(panel, new String(c), Position.LEFT);
		}
	}

	public void onModuleLoad() {
		// es.subscribe("somestuff");
		// MessageService.WebSocket.connect("ws://echo.websocket.org/", new
		// WebSocketHandler() {
		//
		// @Override
		// public void handleOpen(WebSocket ws) {
		// GWT.log("connected to " + ws + ", state: " + ws.getState());
		// ws.send("Hello Mr. Echo Service!");
		// }
		//
		// @Override
		// public void handleMessage(WebSocket ws, String message) {
		// GWT.log("Received WS message: " + message);
		// }
		//
		// @Override
		// public void handleClose(WebSocket ws) {
		// GWT.log("Closed " + ws);
		// }
		//
		// @Override
		// public void handleError(WebSocket ws, String message) {
		// GWT.log("Error: " + message);
		// }
		//
		// });

		mainPanel = new FlowPanel();
		mainPanel.setStyleName("phoenix container");
		RootPanel.get().add(mainPanel);
		avatarPanel = new FlowPanel();
		avatarPanel.setStyleName("avatar-panel");
		mainPanel.add(avatarPanel);
		getConversationAvatarPanel("system").addStyleName("selected");
		getChatPanel("system").setVisible(true);
		speakPanel = new FlowPanel();
		speakPanel.setStyleName("speak-panel");
		mainPanel.add(speakPanel);

		Image phoenixAvatar = new Image("img/phoenix-avatar.png");
		phoenixAvatar.setWidth("128px");
		mainPanel.add(phoenixAvatar);

		// TextBox speakBox = new TextBox();
		// speakBox.getElement().setAttribute("placeholder", "Message");
		// speakPanel.add(speakBox);
		// speakBox.addKeyUpHandler(new KeyUpHandler() {
		//
		// @Override
		// public void onKeyUp(KeyUpEvent event) {
		// if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
		// String text = ((TextBox) event.getSource()).getText();
		// // speak(text, Position.RIGHT);
		// ((TextBox) event.getSource()).setText("");
		// // selectedConversationSession.send(text);
		// }
		// }
		// });

		// speak("Welcome to Phoenix!", Position.LEFT);
		// final Label initializingLabel = speak("Initializing Cipher Suite...",
		// Position.LEFT);
		CipherSuite.init(new Callback<Void, Exception>() {
			private Conversation conversation;

			@Override
			public void onFailure(Exception reason) {
			}

			@Override
			public void onSuccess(Void result) {
				// initializingLabel.setText(initializingLabel.getText() +
				// "done");

				if (Window.Location.getParameter("pub") != null && Window.Location.getParameter("priv") != null) {
					// speak("Key pair found in request.", Position.LEFT);
					final KeyPair keyPair = AsymmetricCipher.Factory.createKeyPair(
							URL.decodePathSegment(Window.Location.getParameter("pub")),
							URL.decodePathSegment(Window.Location.getParameter("priv")));
					speak(getChatPanel("system"), keyPair.getPublicKey(), "Public key: ", Position.LEFT);
					speak(getChatPanel("system"), keyPair.getPrivateKey(), "Private key: ", Position.LEFT);
					// beginClientSession(keyPair);
					clientSession = ClientSession.builder().keyPair(keyPair)
							.conversationInitiationHandler(c -> handleNewConversation(c)).build();
					// Conversation conversation =
					// clientSession.startConversation();
					speak(getChatPanel("system"), createInvitePanel(keyPair), Position.LEFT);
				} else {
					final Label keyPairMessage = speak(getChatPanel("system"), "Generating key pair...", Position.LEFT);
					AsymmetricCipher.Factory.generateKeyPairAsync(KeyPair.KeyStrength.MEDIUM, PublicExponent.SMALLEST,
							new Callback<KeyPair, Void>() {

						@Override
						public void onFailure(Void reason) {
							keyPairMessage.setText(keyPairMessage.getText() + "failed");
						}

						@Override
						public void onSuccess(KeyPair keyPair) {
							keyPairMessage.setText(keyPairMessage.getText() + "done");
							Anchor link = new Anchor("Use your new key pair");
							link.setHref(com.google.gwt.core.client.GWT.getHostPageBaseURL() + "?priv="
									+ URL.encodePathSegment(keyPair.getPrivateKey().getEncodedKey()) + "&pub="
									+ URL.encodePathSegment(keyPair.getPublicKey().getEncodedKey()));
							speak(getChatPanel("system"), link, Position.LEFT);
							speak(getChatPanel("system"), keyPair.getPublicKey(), "Public key: ", Position.LEFT);
							speak(getChatPanel("system"), keyPair.getPrivateKey(), "Private key: ", Position.LEFT);
							// beginClientSession(result);
							clientSession = ClientSession.builder().keyPair(keyPair)
									.conversationInitiationHandler(c -> handleNewConversation(c)).build();
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

	private Map<String, Panel> chatPanels = new HashMap<String, Panel>();

	private Panel getChatPanel(String conversationId) {
		Panel chatPanel = chatPanels.get(conversationId);
		if (chatPanel == null) {
			chatPanel = new FlowPanel();
			chatPanel.setVisible(false);
			chatPanel.setStyleName("chat-panel");
			mainPanel.add(chatPanel);
			chatPanels.put(conversationId, chatPanel);
			if (!conversationId.equals("system")) {
				TextBox speakBox = new TextBox();
				speakBox.setStylePrimaryName("speak");
				speakBox.getElement().setAttribute("placeholder", "Message");
				chatPanel.add(speakBox);
				speakBox.addKeyUpHandler(new KeyUpHandler() {

					@Override
					public void onKeyUp(KeyUpEvent event) {
						if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
							String text = ((TextBox) event.getSource()).getText();
							// speak(text, Position.RIGHT);
							((TextBox) event.getSource()).setText("");
							Conversation conversation = clientSession.getConversation(conversationId);
							conversation.send(text);
						}
					}
				});
			}
		}
		return chatPanel;
	}

	// private Map<String, ConversationSession> conversationSessions = new
	// HashMap<String, ConversationSession>();

	// private void beginClientSession(final KeyPair keyPair) {
	// clientSession = new ClientSession(keyPair,
	// asList(keyPair.getPublicKey()));
	//
	// speak(getChatPanel("system"), createInvitePanel(keyPair), Position.LEFT);
	// // speak(importPublicKeyPanel(), Position.LEFT);
	//
	// GWT.log("starting client session");
	// clientSession.start(new InvitationCallback() {
	// @Override
	// public void handleInvitation(final ConversationSession conversation) {
	//
	// getConversationAvatarPanel(conversation.getId());
	// final Panel chatPanel = getChatPanel(conversation.getId());
	// chatPanel.setVisible(false);
	//
	// conversationSessions.put(conversation.getId(), conversation);
	//
	// conversation.start(new MessageHandler() {
	//
	// @Override
	// public void handleReceivedMessage(Message message, byte[] content) {
	// try {
	// String messageString = new String(content, "UTF-8");
	// Label bubble;
	// if (conversation.isSender(message.getSenderId())) {
	// bubble = speak(chatPanel, messageString, Position.RIGHT);
	// } else {
	// bubble = speak(chatPanel, messageString, Position.LEFT);
	// }
	// bubble.setTitle(getFormat(DATE_TIME_MEDIUM).format(message.getTimestamp()));
	// bubble.getElement().setId(message.getId());
	// } catch (UnsupportedEncodingException e) {
	// throw new IllegalStateException(e);
	// }
	// }
	// });
	// }
	//
	// });
	// }

	private Map<String, Panel> conversationAvatarPanels = new HashMap<String, Panel>();

	private Panel getConversationAvatarPanel(final String conversationId) {
		GWT.log("new conversation: " + conversationId);
		Panel conversationPanel = conversationAvatarPanels.get(conversationId);
		if (conversationPanel == null) {
			conversationPanel = new FlowPanel();
			conversationPanel.getElement().setClassName("conversation");
			if (conversationId.equals("system")) {
				conversationPanel.add(new Image("img/phoenix-avatar.png"));
				conversationPanel.add(new Label("Phoenix"));
			} else {
				conversationPanel.add(new Image("https://www.gravatar.com/avatar/ef11f4d918bcb95c61ac900db78d080f"));
				conversationPanel.add(new Label("Unknown"));
			}
			avatarPanel.add(conversationPanel);
			conversationAvatarPanels.put(conversationId, conversationPanel);
			conversationPanel.addDomHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					for (Entry<String, Panel> e : conversationAvatarPanels.entrySet()) {
						if (e.getKey().equals(conversationId)) {
							e.getValue().addStyleName("selected");
						} else {
							e.getValue().removeStyleName("selected");
						}
					}
					for (Entry<String, Panel> e : chatPanels.entrySet()) {
						if (e.getKey().equals(conversationId)) {
							e.getValue().setVisible(true);
						} else {
							e.getValue().setVisible(false);
						}
					}
					// selectedConversationSession =
					// conversationSessions.get(conversationId);
					// GWT.log("Current conversation: " +
					// selectedConversationSession.getId());
				}
			}, ClickEvent.getType());
		}
		return conversationPanel;
	}

	private FlowPanel createInvitePanel(final KeyPair keyPair) {
		FlowPanel invitePanel = new FlowPanel();
		invitePanel.add(new Label("Invite: "));
		final TextBox publicKeyTextBox = new TextBox();
		publicKeyTextBox.getElement().setAttribute("placeholder", "Public Key");
		publicKeyTextBox.setText(
				"OoKooIqK7J2Pccr7ERP/9BDgJDHDfdCTqbQAPaYMj0YmG9oA4Q3TofBDRMq4gpkTvCrxXuAY/qHpNEbxgMqTlfeXyPN7HT4UrmavHidnMVqgXAuSxX1Kmw6ThJoy5UEVdcrBdc0BSkaQjoni32XG0zOnEOvGPdswxVkbKydc5Y5+X7lsAFJZA6FpFdPAIoYanXTgMp39re3HhAYDkJmsgQ==");
		invitePanel.add(publicKeyTextBox);
		publicKeyTextBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !publicKeyTextBox.getText().isEmpty()) {
					PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(publicKeyTextBox.getText());
					GWT.log("Inviting " + publicKey.getId());
					Conversation conversation = clientSession.startConversation();
					conversation.introduce(publicKey);
					new Timer() {
						@Override
						public void run() {
							conversation.send("Hello");
						}
					}.schedule(1000);
					// selectedConversationSession = clientSession
					// .beginConversation(asList(keyPair.getPublicKey().getId()));
					// selectedConversationSession.invite(publicKey);
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
