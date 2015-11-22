package de.kune.phoenix.client.messaging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.MutableKeyStore;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SimpleKeyStore;
import de.kune.phoenix.client.crypto.util.Base64Utils;
import de.kune.phoenix.client.messaging.PollingRestMessageReceiver.DecryptedMessageHandler;
import de.kune.phoenix.shared.Message;
import de.kune.phoenix.shared.Message.Type;

public class ClientSession {

	private final MessageService messageService = MessageService.instance();
	private final KeyPair keyPair;
	private InvitationCallback invitationCallback;
	private SimpleKeyStore keyStore;

	public ClientSession(KeyPair keyPair, Collection<PublicKey> knownPublicKeys) {
		keyStore = new SimpleKeyStore();
		this.keyPair = keyPair;
		for (PublicKey publicKey : knownPublicKeys) {
			keyStore.add(publicKey);
		}
		keyStore.add(keyPair);
	}

	/**
	 * Begin a conversation with the specified participants.
	 * 
	 * @param participantIds
	 *            the participants
	 * @return a new conversation session
	 */
	public ConversationSession beginConversation(Collection<String> participantIds) {
		ConversationSession conversation = new ConversationSession(this, keyPair, participantIds);
		GWT.log("beginning conversation: " + conversation.getId());
		// conversation.start(messageCallback);
		for (String participantId : participantIds) {
			conversation.invite(keyStore.getPublicKey(participantId));
		}
		return conversation;
	}

	/**
	 * Introduces the sender to the recipients.
	 * 
	 * @param recipientIds
	 *            the recipients
	 */
	public void postSelfIntroduction(Collection<String> recipientIds) {
		for (final String recipientId : recipientIds) {
			PublicKey recipientPublicKey = keyStore.getPublicKey(recipientId);
			if (recipientPublicKey == null) {
				throw new IllegalStateException("unknown recipient <" + recipientId + ">");
			}
			Message introduction = new Message();
			introduction.setMessageType(Type.INTRODUCTION);
			introduction.setRecipientIds(new String[] { recipientId });
			introduction.setContent(keyPair.getPublicKey().getPlainKey());
			introduction.setSenderId(keyPair.getPublicKey().getId());
			introduction.setTimestamp(new Date());
			introduction.sign(keyPair.getPrivateKey());
			GWT.log("sending introduction: " + introduction.toString());
			messageService.post(introduction, new MethodCallback<Void>() {
				@Override
				public void onSuccess(Method method, Void response) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					throw new RuntimeException("failed to introduce self to " + recipientId + "", exception);
				}
			});
		}
	}

	public void start(InvitationCallback invitationCallback) {
		this.invitationCallback = invitationCallback;
		final Processor<Message> decryptorSession = new MessageDecryptorSession(keyStore,
				new DecryptedMessageHandler() {
					@Override
					public void handleMessage(Message message, byte[] plainContent) {
						handleIncomingMessage(message, plainContent);
					}
				});
		GWT.log("Starting polling rest message receiver");
		new PollingRestMessageReceiver(keyPair.getPublicKey().getId(), decryptorSession).start();
		MessageService service = GWT.create(MessageService.class);
		service.receive(null, null, new Callback<Collection<Message>, String>() {
			@Override
			public void onSuccess(Collection<Message> result) {
				GWT.log("Received ES message(s): " + result.toString());
				// decryptorSession.process(result);
			}

			@Override
			public void onFailure(String reason) {
				// TODO Auto-generated method stub
			}
		});
	}

	protected void handleIncomingMessage(final Message message, byte[] plainContent) {
		if (message.getMessageType() == Type.INVITATION) {
			final byte[] conversationId = plainContent;
			new Timer() {
				public void run() {
					invitationCallback.handleInvitation(new ConversationSession(ClientSession.this, conversationId,
							keyPair, Arrays.asList(keyPair.getPublicKey().getId(), message.getSenderId())));
				}
			}.schedule(0);
		} else if (message.getMessageType() == Type.INTRODUCTION) {
			PublicKey receivedPublicKey = AsymmetricCipher.Factory
					.createPublicKey(Base64Utils.encode(message.getContent()));
			GWT.log("received public key: " + receivedPublicKey.getEncodedKey());
			if (!message.getSenderId().equals(receivedPublicKey.getId())) {
				throw new IllegalStateException("received foreign public key <" + receivedPublicKey.getId() + "> from <"
						+ message.getSenderId() + ">");
			}
			if (!message.checkSignature(receivedPublicKey)) {
				throw new IllegalStateException("verification of received public key <" + receivedPublicKey.getId()
						+ "> failed, invalid signature");
			}
			importPublicKey(receivedPublicKey);
		}

	}

	public KeyPair getKeyPair() {
		return keyPair;
	}

	public void importPublicKey(PublicKey publicKey) {
		keyStore.add(publicKey);
	}

	public MutableKeyStore getKeyStore() {
		return keyStore;
	}

	public void postInvitaition(String participantId, String conversationSessionId) {
		Message invitation = new Message();
		invitation.setMessageType(Type.INVITATION);
		invitation.setRecipientIds(new String[] { participantId });
		invitation.setSenderId(keyPair.getPublicKey().getId());
		invitation.setKeyId(participantId);
		invitation.setAndEncryptContent(keyStore.getPublicKey(participantId),
				Base64Utils.decode(conversationSessionId));
		invitation.setTimestamp(new Date());
		invitation.sign(keyPair.getPrivateKey());
		GWT.log("sending invitation: " + invitation.toString());
		messageService.post(invitation, new MethodCallback<Void>() {
			@Override
			public void onSuccess(Method method, Void response) {
			}

			@Override
			public void onFailure(Method method, Throwable exception) {
			}
		});
	}

}
