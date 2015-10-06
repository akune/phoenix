package de.kune.phoenix.client.messaging;

import java.util.List;

import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.shared.Message;

public class PollingRestMessageReceiver {

	private static final int pollInterval = 5000;

	public static interface DecryptedMessageHandler {
		void handleMessage(Message message, byte[] content);
	}

	private final Timer pollingRestReceiverTimer = new Timer() {

		@Override
		public void run() {
			MethodCallback<List<Message>> messageHandler = new MethodCallback<List<Message>>() {
				@Override
				public void onSuccess(Method method, List<Message> response) {
					GWT.log("Received " + response);
					decryptorSession.process(response);
					pollingRestReceiverTimer.schedule(pollInterval);
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					pollingRestReceiverTimer.schedule(5000);
					throw new RuntimeException(exception);
				}
			};
			GWT.log("Querying for messages with transmission greater than "
					+ (decryptorSession.getLastProcessedObject() == null ? null
							: decryptorSession.getLastProcessedObject().getTransmission())
					+ ", receiverId=" + receiverId + ", conversationId=" + conversationId);
			messageService.get(false,
					decryptorSession.getLastProcessedObject() == null ? null
							: decryptorSession.getLastProcessedObject().getTransmission(),
					receiverId, conversationId, messageHandler);
		}
	};

	private final MessageService messageService = MessageService.instance();

	private String receiverId;

	private String conversationId;

	private Processor<Message> decryptorSession;

	public PollingRestMessageReceiver(String receiverId, Processor<Message> decryptorSession) {
		this.receiverId = receiverId;
		this.decryptorSession = decryptorSession;
	}

	public PollingRestMessageReceiver(String receiverId, String conversationId,
			MessageDecryptorSession decryptorSession) {
		this(receiverId, decryptorSession);
		this.conversationId = conversationId;
	}

	public void start() {
		GWT.log("Starting polling rest receiver timer in " + pollInterval + "ms");
		pollingRestReceiverTimer.schedule(pollInterval);
	}

}
