package de.kune.phoenix.client.messaging;

import static java.util.Arrays.asList;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;

import de.kune.phoenix.client.crypto.AsymmetricCipher;
import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.crypto.PublicKey;
import de.kune.phoenix.client.crypto.SecretKey;
import de.kune.phoenix.client.crypto.SecretKey.KeyStrength;
import de.kune.phoenix.client.crypto.SimpleKeyStore;
import de.kune.phoenix.client.crypto.SymmetricCipher;
import de.kune.phoenix.client.crypto.util.Base64Utils;
import de.kune.phoenix.client.crypto.util.Sha256;
import de.kune.phoenix.client.messaging.PollingRestMessageReceiver.DecryptedMessageHandler;
import de.kune.phoenix.shared.Message;
import de.kune.phoenix.shared.Message.Type;

public class ConversationSession {

	private static final MethodCallback<Void> noActionMethodCallback = new MethodCallback<Void>() {
		@Override
		public void onFailure(Method method, Throwable exception) {
		}

		@Override
		public void onSuccess(Method method, Void response) {
		}
	};

	private final MessageService messageService = MessageService.instance();
	private String conversationId;
	private SimpleKeyStore conversationKeyStore;
	private KeyPair sessionKeyPair;
	private Map<String, PublicKey> participantsPublicKeys = new LinkedHashMap<>();
	private MessageCallback messageCallback;
	private ClientSession clientSession;

	public ConversationSession(ClientSession clientSession, KeyPair keyPair, Collection<String> participantIds) {
		this(clientSession, generateSessionId(), keyPair, participantIds);
	}

	public ConversationSession(ClientSession clientSession, byte[] sessionId, KeyPair keyPair,
			Collection<String> participantIds) {
		conversationKeyStore = new SimpleKeyStore(clientSession.getKeyStore());
		this.clientSession = clientSession;
		setConversationId(sessionId);
		this.sessionKeyPair = keyPair;
		addParticipants(participantIds);
	}

	public String getId() {
		return conversationId;
	}

	private void addParticipants(Collection<String> participantIds) {
		for (String participantId : participantIds) {
			PublicKey participantPublicKey = clientSession.getKeyStore().getPublicKey(participantId);
			if (participantPublicKey == null) {
				throw new IllegalArgumentException(
						"cannot add participant - unknown public key <" + participantId + ">");
			}
			this.participantsPublicKeys.put(participantId, participantPublicKey);
		}

	}

	private static byte[] generateSessionId() {
		byte[] random = new byte[255];
		new Random().nextBytes(random);
		return new Sha256().feed("conversation").feed(Long.toString(System.currentTimeMillis())).feed(random)
				.toByteArray();
	}

	private void setConversationId(byte[] conversationId) {
		this.conversationId = Base64Utils.encode(conversationId);
	}

	public void start(MessageCallback callback) {
		this.messageCallback = callback;
		MessageDecryptorSession decryptorSession = new MessageDecryptorSession(conversationKeyStore,
				new DecryptedMessageHandler() {
					@Override
					public void handleMessage(Message message, byte[] plainContent) {
						handleIncomingMessage(message, plainContent);
					}
				});
		new PollingRestMessageReceiver(sessionKeyPair.getPublicKey().getId(), conversationId, decryptorSession).start();
	}

	/**
	 * Introduces a new participant to this conversation.
	 */
	public void introduce(String participantId) {
		SecretKey secretKey = getSecretKey();
		postIntroduction(participantId, secretKey);
	}

	private void postIntroduction(String participantId, SecretKey secretKey) {
		Message message = new Message();
		message.setMessageType(Type.INTRODUCTION);
		message.setSenderId(sessionKeyPair.getPublicKey().getId());
		message.setConversationId(getId());
		message.setKeyId(secretKey.getId());
		message.setAndEncryptContent(secretKey, conversationKeyStore.getPublicKey(participantId).getPlainKey());
		message.setTimestamp(new Date());
		message.sign(sessionKeyPair.getPrivateKey());
		messageService.post(message, noActionMethodCallback);
	}

	private SecretKey getSecretKey() {
		SecretKey secretKey = conversationKeyStore.getLatestSecretKey();
		if (secretKey == null) {
			secretKey = SymmetricCipher.Factory.generateSecretKey(KeyStrength.STRONGEST);
			conversationKeyStore.add(secretKey);
			for (Entry<String, PublicKey> recipientPublicKey : participantsPublicKeys.entrySet()) {
				sendSecretKey(secretKey, recipientPublicKey.getKey());
			}
		}
		return secretKey;
	}

	public void invite(final PublicKey participantPublicKey) {
		clientSession.getKeyStore().add(participantPublicKey);
		addParticipants(asList(participantPublicKey.getId()));
		introduce(participantPublicKey.getId());
		for (SecretKey key : conversationKeyStore.getAllSecretKeys()) {
			sendSecretKey(key, participantPublicKey.getId());
		}
		clientSession.postSelfIntroduction(asList(participantPublicKey.getId()));
		clientSession.postInvitaition(participantPublicKey.getId(), getId());
	}

	public void send(String content) {
		SecretKey key = getSecretKey();
		Message message = new Message();
		message.setSenderId(sessionKeyPair.getPublicKey().getId());
		message.setConversationId(getId());
		message.setMessageType(Message.Type.PLAIN_TEXT);
		try {
			message.setAndEncryptContent(key, content.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		message.setKeyId(key.getId());
		message.setTimestamp(new Date());
		message.sign(sessionKeyPair.getPrivateKey());
		messageService.post(message, noActionMethodCallback);
	}

	private void sendSecretKey(SecretKey key, String recipientId) {
		Message keyMessage = new Message();
		keyMessage.setSenderId(sessionKeyPair.getPublicKey().getId());
		keyMessage.setConversationId(getId());
		keyMessage.setMessageType(Message.Type.SECRET_KEY);
		keyMessage.setAndEncryptContent(participantsPublicKeys.get(recipientId), key.getPlainKey());
		keyMessage.setKeyId(recipientId);
		keyMessage.setRecipientIds(new String[] { recipientId });
		keyMessage.setTimestamp(new Date());
		keyMessage.sign(sessionKeyPair.getPrivateKey());
		GWT.log("sending secret key message to <" + conversationId + ">: " + keyMessage);
		messageService.post(keyMessage, noActionMethodCallback);
	}

	private void handleIncomingMessage(Message message, byte[] content) {
		validateSender(message);
		if (message.getMessageType() == Message.Type.SECRET_KEY) {
			SecretKey secretKey = SymmetricCipher.Factory.createSecretKey(content);
			conversationKeyStore.add(secretKey);
		} else if (message.getMessageType() == Message.Type.PLAIN_TEXT) {
			try {
				GWT.log("Received: " + new String(content, "UTF-8"));
				messageCallback.handleReceivedMessage(message, content);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		} else if (message.getMessageType() == Message.Type.INTRODUCTION) {
			PublicKey publicKey = AsymmetricCipher.Factory.createPublicKey(Base64Utils.encode(content));
			clientSession.getKeyStore().add(publicKey);
			addParticipants(asList(publicKey.getId()));
		}
	}

	private void validateSender(Message message) {
		if (!participantsPublicKeys.containsKey(message.getSenderId())) {
			throw new IllegalStateException("sender id <" + message.getSenderId()
					+ "> is not a member of this conversation, members: " + participantsPublicKeys.keySet());
		}
	}

	public boolean isSender(String participantId) {
		return sessionKeyPair.getPublicKey().getId().equals(participantId);
	}

}
