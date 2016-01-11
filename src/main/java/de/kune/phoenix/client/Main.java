package de.kune.phoenix.client;

import static de.kune.phoenix.client.Animations.fadeIn;

import java.io.UnsupportedEncodingException;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.CipherSuite;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.KeyPair.PublicExponent;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.messaging.ClientSession;
import de.kune.phoenix.client.messaging.Conversation;
import de.kune.phoenix.shared.Message;

public class Main implements EntryPoint {

	private ClientSession clientSession;
	private ChatClientWidget chatClientWidget;
	private AreaTooSmallWidget areaTooSmallWidget;

	private void handleNewConversation(Conversation.Builder builder) {
		GWT.log("handle new conversation");
		chatClientWidget.addConversation(builder.getConversationId(), "Unknown Participant");
		builder.receivedMessageHandler((m, c) -> handleReceivedMessage(builder.getConversationId(), m, c));
	}

	private void handleReceivedMessage(String conversationId, Message m, byte[] c) {
		Conversation conversation = clientSession.getConversation(conversationId);
		if (conversation != null) {
			try {
				if (m.getSenderId().equals(conversation.getSenderId())) {
					chatClientWidget().addSentMessage(conversationId, new String(c, "UTF-8"));
				} else {
					chatClientWidget().addReceivedMessage(conversationId, new String(c, "UTF-8"));
				}
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("utf-8 not supported");
			}
		} else {
			GWT.log("no such conversation");
		}
	}

	private ChatClientWidget chatClientWidget() {
		if (chatClientWidget == null) {
			chatClientWidget = new ChatClientWidget();
			chatClientWidget.setVisible(false);
		}
		return chatClientWidget;
	}

	private Widget areaTooSmallWidget() {
		if (areaTooSmallWidget == null) {
			areaTooSmallWidget = new AreaTooSmallWidget();
		}
		return areaTooSmallWidget;
	}
	
	public void onModuleLoad() {
		Storage storage = Storage.getLocalStorageIfSupported();
		GWT.log("storage supported: " + (storage != null));

		RootPanel.get().add(areaTooSmallWidget());
		RootPanel.get().add(chatClientWidget());
		CipherSuite.init(new Callback<Void, Exception>() {

			@Override
			public void onFailure(Exception reason) {
				GWT.log("could not initialize cipher suite", reason);
			}

			@Override
			public void onSuccess(Void result) {
				fadeIn(chatClientWidget()).run(250);
				getOrCreateKeyPair(new Callback<KeyPair, Exception>() {
					@Override
					public void onFailure(Exception reason) {
					}

					@Override
					public void onSuccess(KeyPair keyPair) {
						clientSession = ClientSession.builder().keyPair(keyPair)
								.conversationInitiationHandler(c -> handleNewConversation(c)).build();
						chatClientWidget.addInformationEntry(keyPair.getPublicKey().getEncodedKey());
						chatClientWidget.setSearchHandler(s -> performSearch(s));
						chatClientWidget.setSendMessageHandler(
								(conversationId, message) -> sendMessage(conversationId, message));
					}
				});
			}
		});
	}

	protected void sendMessage(String conversationId, String message) {
		Conversation conversation = clientSession.getConversation(conversationId);
		if (conversation != null) {
			conversation.send(message);
		} else {
			GWT.log("no such conversation");
		}
	}

	protected void performSearch(String searchString) {
		GWT.log("handling search...");
		try {
			PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(searchString);
			GWT.log("Inviting " + publicKey.getId());
			Conversation conversation = clientSession.startConversation();
			conversation.introduce(publicKey);
		} catch (RuntimeException e) {
			GWT.log("not a public key");
		}
	}

	protected void getOrCreateKeyPair(Callback<KeyPair, Exception> callback) {
		KeyPair keyPair = getKeyPairFromRequestParams();
		if (keyPair == null) {
			AsymmetricCipher.Factory.generateKeyPairAsync(KeyPair.KeyStrength.MEDIUM, PublicExponent.SMALLEST,
					new Callback<KeyPair, Void>() {
						@Override
						public void onFailure(Void reason) {
							callback.onFailure(new IllegalStateException("could not generate key pair"));
						}

						@Override
						public void onSuccess(KeyPair result) {
							callback.onSuccess(result);
						}
					}, new Callback<Integer, Void>() {
						@Override
						public void onFailure(Void reason) {
							callback.onFailure(new IllegalStateException("could not generate key pair"));
						}

						@Override
						public void onSuccess(Integer result) {
						}
					});
		} else {
			callback.onSuccess(keyPair);
		}
	}

	protected KeyPair getKeyPairFromRequestParams() {
		if (Window.Location.getParameter("pub") != null && Window.Location.getParameter("priv") != null) {
			final KeyPair keyPair = AsymmetricCipher.Factory.createKeyPair(
					URL.decodePathSegment(Window.Location.getParameter("pub")),
					URL.decodePathSegment(Window.Location.getParameter("priv")));
			return keyPair;
		}
		return null;
	}

}
