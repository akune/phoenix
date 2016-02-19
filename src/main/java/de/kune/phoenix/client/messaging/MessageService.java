package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.functional.FailureHandler;
import de.kune.phoenix.client.functional.MessageHandler;
import de.kune.phoenix.client.functional.Predicate;
import de.kune.phoenix.client.functional.SuccessHandler;
import de.kune.phoenix.client.messaging.communication.RestMessageService;
import de.kune.phoenix.shared.Message;

public class MessageService {

	private static final MessageService instance = new MessageService();
	private static final int pollInterval = 250;

	public static MessageService instance() {
		return instance;
	}

	private final RestMessageService restMessageService;
	private final Timer pollingRestReceiverTimer = new Timer() {
		private int connectionFailureCount = 0;

		@Override
		public void run() {
			MethodCallback<List<Message>> messageHandler = new MethodCallback<List<Message>>() {

				@Override
				public void onSuccess(Method method, List<Message> response) {
					if (!connected) {
						GWT.log("(re-)established connection");
						connected = true;
						connectionFailureCount = 0;
						invokeConnectionStateChangeHandlers();
					}
					GWT.log("received messages " + response);
					for (Message m : response) {
						handleReceivedMessage(m);
					}
					pollingRestReceiverTimer.schedule(pollInterval);
				}

				@Override
				public void onFailure(Method method, Throwable exception) {
					connectionFailureCount++;
					if (connected) {
						GWT.log("lost connection");
						connected = false;
						invokeConnectionStateChangeHandlers();
					}
					int delay = Math.min(connectionFailureCount * connectionFailureCount * 250, 60 * 1000);
					GWT.log("getting latest message(s) failed " + connectionFailureCount + " times, delaying retry for "
							+ delay + "ms");
					pollingRestReceiverTimer.schedule(delay);
					// throw new RuntimeException(exception);
				}
			};
			restMessageService.get(connected, lastReceivedMessage == null ? null : lastReceivedMessage.getSequenceKey(),
					recipientId, messageHandler);
		}
	};

	public static interface ConnectionStateChangeHandler {
		void handleConnectionStateChange(MessageService service);
	}

	private boolean connected;
	private List<ConnectionStateChangeHandler> connectionStateChangeHandlers = new ArrayList<>();
	private String recipientId;
	private Map<Predicate<Message>, MessageHandler> messageHandlers = new LinkedHashMap<>();
	private Message lastReceivedMessage;
	private List<Message> queuedMessages = new ArrayList<>();

	private MessageService() {
		Defaults.setServiceRoot(com.google.gwt.core.client.GWT.getModuleBaseURL()
				.replace(com.google.gwt.core.client.GWT.getModuleName() + "/", "") + "api");
		Defaults.setDateFormat(null);
		restMessageService = GWT.create(RestMessageService.class);
	}

	private void invokeConnectionStateChangeHandlers() {
		GWT.log("invoking connection state change handlers");
		for (ConnectionStateChangeHandler h : connectionStateChangeHandlers) {
			h.handleConnectionStateChange(this);
		}
	}

	public void addConnectionStateChangeHandler(ConnectionStateChangeHandler h) {
		connectionStateChangeHandlers.add(h);
	}

	public void enqueue(List<Message> messages) {
		queuedMessages.addAll(messages);
	}

	/**
	 * Sends a message. Does nothing on success. Logs failures.
	 * 
	 * @param message
	 *            the message
	 */
	public void send(Message message) {
		send(message, SuccessHandler.nothing(), FailureHandler.log());
	}

	/**
	 * Sends a message. Does nothing on success.
	 * 
	 * @param message
	 *            the message
	 * @param failureHandler
	 *            the failure handler
	 */
	public void send(Message message, FailureHandler<Message> failureHandler) {
		send(message, SuccessHandler.nothing(), failureHandler);
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            the message
	 * @param successHandler
	 *            the success handler
	 * @param failureHandler
	 *            the failure handler
	 */
	public void send(Message message, SuccessHandler<Message> successHandler, FailureHandler<Message> failureHandler) {
		send(Arrays.<Message> asList(message), successHandler, failureHandler);
	}

	/**
	 * Sends messages. Does nothing on success. Logs failures.
	 * 
	 * @param messages
	 *            the messages
	 */
	public void send(List<Message> messages) {
		send(messages, SuccessHandler.nothing(), FailureHandler.log());
	}

	/**
	 * Sends messages. Will retry {@link retryCount} times after failure.
	 * 
	 * @param messages
	 *            the messages
	 * @param retryCount
	 *            the retry count after failure
	 */
	public void send(List<Message> messages, int retryCount) {
		if (retryCount == 0) {
			send(messages);
		} else {
			send(messages, SuccessHandler.nothing(), (t, m) -> send(messages, retryCount - 1));
		}
	}

	/**
	 * Sends messages. Does nothing on success.
	 * 
	 * @param messages
	 *            the messages
	 * @param failureHandler
	 *            the failure handler
	 */
	public void send(List<Message> messages, FailureHandler<Message> failureHandler) {
		send(messages, SuccessHandler.nothing(), failureHandler);
	}

	/**
	 * Sends messages.
	 * 
	 * @param messages
	 *            the messages
	 * @param successHandler
	 *            the success handler
	 * @param failureHandler
	 *            the failure handler
	 */
	public void send(List<Message> messages, SuccessHandler<Message> successHandler,
			FailureHandler<Message> failureHandler) {
		List<Message> messagesToSend = new ArrayList<>();
		messagesToSend.addAll(queuedMessages);
		queuedMessages.clear();
		messagesToSend.addAll(messages);
		restMessageService.post(messagesToSend, new MethodCallback<Void>() {
			@Override
			public void onFailure(Method method, Throwable exception) {
				for (Message m : messagesToSend) {
					failureHandler.handle(exception, m);
				}
			}

			@Override
			public void onSuccess(Method method, Void response) {
				for (Message m : messagesToSend) {
					successHandler.handle(m);
				}
			}
		});
	}

	/**
	 * Registers a message handler. Message handlers will be invoked in the same
	 * order as they have been registered.
	 * 
	 * @param predicate
	 *            the predicate to determine if a handler should be invoked for
	 *            a specific message
	 * @param messageHandler
	 *            the message handler
	 */
	public void addMessageHandler(Predicate<Message> predicate, MessageHandler messageHandler) {
		messageHandlers.put(predicate, messageHandler);
	}

	private void handleReceivedMessage(Message message) {
		lastReceivedMessage = message;
		List<MessageHandler> handlers = new ArrayList<>();
		for (Entry<Predicate<Message>, MessageHandler> e : messageHandlers.entrySet()) {
			if (e.getKey().test(message)) {
				handlers.add(e.getValue());
			}
		}
		for (MessageHandler h : handlers) {
			h.handleReceivedMessage(message, null);
		}
	}

	public void start(String recipientId) {
		this.recipientId = recipientId;
		pollingRestReceiverTimer.schedule(0);
	}

	public void stop() {
		pollingRestReceiverTimer.cancel();
	}

	public boolean isConnected() {
		return connected;
	}

}
