package de.kune.phoenix.client;

import static com.google.gwt.i18n.client.DateTimeFormat.getFormat;
import static com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM;
import static java.util.Arrays.asList;

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
import de.kune.phoenix.client.messaging.MessageService;
import de.kune.phoenix.client.messaging.MessageService.EventSource;
import de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler;
import de.kune.phoenix.shared.Message;

public class Main implements EntryPoint {

	private ConversationSession selectedConversationSession;

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

	public void onModuleLoad() {
		MessageService.EventSource.connect("http://localhost:8888/es", new EventSourceHandler() {
			@Override
			public void handleOpen(EventSource es) {
				GWT.log("ES open");
			}
			@Override
			public void handleMessage(EventSource es, String message) {
				GWT.log("Received ES message: " + message);
			}
			@Override
			public void handleClose(EventSource es) {
				GWT.log("ES closed");
			}
			@Override
			public void handleError(EventSource es, String message) {
				GWT.log("ES error: " + message);
			}
		});
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
		speakPanel = new FlowPanel();
		speakPanel.setStyleName("speak-panel");
		mainPanel.add(speakPanel);

		Image phoenixAvatar = new Image("img/phoenix-avatar.png");
		phoenixAvatar.setWidth("128px");
		mainPanel.add(phoenixAvatar);

		TextBox speakBox = new TextBox();
		speakBox.getElement().setAttribute("placeholder", "Message");
		speakPanel.add(speakBox);
		speakBox.addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					String text = ((TextBox) event.getSource()).getText();
					// speak(text, Position.RIGHT);
					((TextBox) event.getSource()).setText("");
					selectedConversationSession.send(text);
				}
			}
		});

		// speak("Welcome to Phoenix!", Position.LEFT);
		// final Label initializingLabel = speak("Initializing Cipher Suite...",
		// Position.LEFT);
		CipherSuite.init(new Callback<Void, Exception>() {
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
					beginClientSession(keyPair);
				} else {
					final Label keyPairMessage = speak(getChatPanel("system"), "Generating key pair...", Position.LEFT);
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
							speak(getChatPanel("system"), link, Position.LEFT);
							speak(getChatPanel("system"), result.getPublicKey(), "Public key: ", Position.LEFT);
							speak(getChatPanel("system"), result.getPrivateKey(), "Private key: ", Position.LEFT);
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

	private Map<String, Panel> chatPanels = new HashMap<String, Panel>();

	private Panel getChatPanel(String conversationId) {
		Panel chatPanel = chatPanels.get(conversationId);
		if (chatPanel == null) {
			chatPanel = new FlowPanel();
			chatPanel.setStyleName("chat-panel");
			mainPanel.add(chatPanel);
			chatPanels.put(conversationId, chatPanel);
		}
		return chatPanel;
	}

	private Map<String, ConversationSession> conversationSessions = new HashMap<String, ConversationSession>();

	private void beginClientSession(final KeyPair keyPair) {
		clientSession = new ClientSession(keyPair, asList(keyPair.getPublicKey()));

		speak(getChatPanel("system"), createInvitePanel(keyPair), Position.LEFT);
		// speak(importPublicKeyPanel(), Position.LEFT);

		GWT.log("starting client session");
		clientSession.start(new InvitationCallback() {
			@Override
			public void handleInvitation(final ConversationSession conversation) {

				getConversationAvatarPanel(conversation.getId());
				final Panel chatPanel = getChatPanel(conversation.getId());
				chatPanel.setVisible(false);

				conversationSessions.put(conversation.getId(), conversation);

				conversation.start(new MessageCallback() {

					@Override
					public void handleReceivedMessage(Message message, byte[] content) {
						try {
							String messageString = new String(content, "UTF-8");
							Label bubble;
							if (conversation.isSender(message.getSenderId())) {
								bubble = speak(chatPanel, messageString, Position.RIGHT);
							} else {
								bubble = speak(chatPanel, messageString, Position.LEFT);
							}
							bubble.setTitle(getFormat(DATE_TIME_MEDIUM).format(message.getTimestamp()));
							bubble.getElement().setId(message.getId());
						} catch (UnsupportedEncodingException e) {
							throw new IllegalStateException(e);
						}
					}
				});
			}

		});
	}

	private Map<String, Panel> conversationAvatarPanels = new HashMap<String, Panel>();

	private Panel getConversationAvatarPanel(final String conversationId) {
		Panel conversationPanel = conversationAvatarPanels.get(conversationId);
		if (conversationPanel == null) {
			conversationPanel = new FlowPanel();
			conversationPanel.getElement().setClassName("conversation");
			if (conversationId.equals("system")) {
				conversationPanel.add(new Image("img/phoenix-avatar.png"));
				conversationPanel.add(new Label("Phoenix"));
			} else {
				conversationPanel.add(new Image("http://www.gravatar.com/avatar/ef11f4d918bcb95c61ac900db78d080f"));
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
					selectedConversationSession = conversationSessions.get(conversationId);
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
		invitePanel.add(publicKeyTextBox);
		publicKeyTextBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !publicKeyTextBox.getText().isEmpty()) {
					PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(publicKeyTextBox.getText());
					GWT.log("Inviting " + publicKey.getId());
					selectedConversationSession = clientSession
							.beginConversation(asList(keyPair.getPublicKey().getId()));
					selectedConversationSession.invite(publicKey);
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
