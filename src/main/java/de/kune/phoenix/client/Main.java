package de.kune.phoenix.client;

import static de.kune.phoenix.client.Animations.fadeIn;

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
import de.kune.phoenix.client.messaging.MessageService;
import de.kune.phoenix.shared.Message;

public class Main implements EntryPoint {

	private static final String STORAGE_PREFIX = "de.kune.phoenix.client";
	private ClientSession clientSession;
	private ChatClientWidget chatClientWidget;
	private AreaTooSmallWidget areaTooSmallWidget;

	private void handleConnectionStateChange(MessageService s) {
		GWT.log("Connected: " + s.isConnected());
		chatClientWidget().setConnectionState(s.isConnected());
	}

	private void handleServerIdentifierChange(String oldIdentifier, String newIdentifier, MessageService s) {
		GWT.log("Server identifier change: old=" + oldIdentifier + ", new=" + newIdentifier);
		chatClientWidget().invalidate();
	}

	private void handleNewConversation(Conversation.Builder builder) {
		GWT.log("handle new conversation");
		chatClientWidget.addConversation(builder.getConversationId(),
				"Unknown Participant");
		builder.chatClientWidget(chatClientWidget);
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
								.conversationInitiationHandler(Main.this::handleNewConversation)
								.connectionStateChangeHander(Main.this::handleConnectionStateChange)
								.serverIdentifierChangeHandler(Main.this::handleServerIdentifierChange)
								.build();
						chatClientWidget.setKeyPair(keyPair);
						chatClientWidget.setSearchHandler(s -> performSearch(s));
						chatClientWidget.setSendMessageHandler(
								(conversationId, message) -> sendMessage(conversationId, message));
					}
				});
			}
		});
	}

	protected Message sendMessage(String conversationId, String message) {
		Conversation conversation = clientSession.getConversation(conversationId);
		if (conversation != null) {
			return conversation.send(message);
		} else {
			GWT.log("no such conversation");
			return null;
		}
	}

	protected void performSearch(String searchString) {
		GWT.log("handling search...");
		try {
			PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(searchString);
			GWT.log("Inviting " + publicKey.getId());
			Conversation conversation = clientSession.startConversation(publicKey);
			chatClientWidget().closeSearchPanel();
			chatClientWidget().activateConversation(conversation.getConversationId());
		} catch (RuntimeException e) {
			GWT.log("not a public key");
		}
	}

	protected void getOrCreateKeyPair(Callback<KeyPair, Exception> callback) {
		KeyPair keyPair = null;
		if (keyPair == null) {
			keyPair = getKeyPairFromRequestParams();
		}
		if (keyPair == null) {
			keyPair = getKeyPairFromLocalStorage();
		}
		if (keyPair == null) {
			AsymmetricCipher.Factory.generateKeyPairAsync(KeyPair.KeyStrength.MEDIUM, PublicExponent.SMALLEST,
					new Callback<KeyPair, Void>() {
						@Override
						public void onFailure(Void reason) {
							callback.onFailure(new IllegalStateException("could not generate key pair"));
						}

						@Override
						public void onSuccess(KeyPair result) {
							addKeyPairToLocalStorage(result);
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

	protected void addKeyPairToLocalStorage(KeyPair result) {
		Storage storage = Storage.getLocalStorageIfSupported();
		GWT.log("storage supported: " + (storage != null));
		if (storage == null) {
			return;
		}
		storage.setItem(STORAGE_PREFIX + "#private-key", result.getPrivateKey().getEncodedKey());
		storage.setItem(STORAGE_PREFIX + "#public-key", result.getPublicKey().getEncodedKey());
	}

	private KeyPair getKeyPairFromLocalStorage() {
		Storage storage = Storage.getLocalStorageIfSupported();
		GWT.log("storage supported: " + (storage != null));
		if (storage == null) {
			return null;
		}
		String encodedPrivateKey = storage.getItem(STORAGE_PREFIX + "#private-key");
		String encodedPublicKey = storage.getItem(STORAGE_PREFIX + "#public-key");
		if (encodedPrivateKey == null || encodedPublicKey == null) {
			return null;
		}
		return AsymmetricCipher.Factory.createKeyPair(encodedPublicKey, encodedPrivateKey);
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
