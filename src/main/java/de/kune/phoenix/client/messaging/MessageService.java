package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.functional.FailureHandler;
import de.kune.phoenix.client.functional.SuccessHandler;
import de.kune.phoenix.client.messaging.communication.RestMessageService;
import de.kune.phoenix.shared.Message;

public class MessageService {

	public static interface ConnectionStateChangeHandler {
		void handleConnectionStateChange(MessageService service);
	}

	public static interface ServerIdentifierChangeHandler {
		void handleServerIdentifierChange(String oldServerIdentifier, String newServerIdenfier, MessageService service);
	}

	private static final MessageService instance = new MessageService();
	private static final int pollInterval = 250;

	public static MessageService instance() {
		return instance;
	}

	private final RestMessageService restMessageService;
	private final Timer pollingRestReceiverTimer = new Timer() {
		private int connectionFailureCount = 0;
		private String lastReceivedSequenceKey;

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
					String receivedServerIdentifier = method.getResponse().getHeader("X-Server-Identifier");
					GWT.log("X-Server-Identifier: " + receivedServerIdentifier);
                    if (serverIdentifier == null || serverIdentifier.equals(receivedServerIdentifier)) {
                        serverIdentifier = receivedServerIdentifier;
						GWT.log("received messages " + response);
						updateLastReceivedSequenceKey(response);
						MessageProcessor.instance().process(response);
						pollingRestReceiverTimer.schedule(pollInterval);
                    } else {
                    	GWT.log("server identifier changed, no more messages will be received or processed");
                        invokeServerIdentifierChangeHandlers(serverIdentifier, receivedServerIdentifier);
                    }
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
				}

				private void updateLastReceivedSequenceKey(List<Message> response) {
					for (Message r : response) {
						if (lastReceivedSequenceKey == null
								|| r.getSequenceKey().compareTo(lastReceivedSequenceKey) > 0) {
							lastReceivedSequenceKey = r.getSequenceKey();
						}
					}
				}

			};
			GWT.log("last received sequence key: " + lastReceivedSequenceKey);
			restMessageService.get(connected, lastReceivedSequenceKey, recipientId, messageHandler);
		}
	};

	private boolean connected;
	private String serverIdentifier;
	private List<ConnectionStateChangeHandler> connectionStateChangeHandlers = new ArrayList<>();
	private List<ServerIdentifierChangeHandler> serverIdentifierChangeHandlers = new ArrayList<>();
	private String recipientId;
	private List<Message> outgoingMessageQueue = new ArrayList<>();

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

	private void invokeServerIdentifierChangeHandlers(String oldServerIdentifier, String newServerIdentifier) {
		GWT.log("invoking server identifier change handlers");
		for (ServerIdentifierChangeHandler h : serverIdentifierChangeHandlers) {
			h.handleServerIdentifierChange(oldServerIdentifier, newServerIdentifier, this);
		}
	}

	public void addServerIdentifierChangeHandler(ServerIdentifierChangeHandler serverIdentifierChangeHandler) {
		serverIdentifierChangeHandlers.add(serverIdentifierChangeHandler);
	}

	public void addConnectionStateChangeHandler(ConnectionStateChangeHandler h) {
		connectionStateChangeHandlers.add(h);
	}

	public List<Message> enqueue(List<Message> messages) {
		outgoingMessageQueue.addAll(messages);
		return messages;
	}

	/**
	 * Sends a message. Does nothing on success. Logs failures.
	 * 
	 * @param message
	 *            the message
	 */
	public Message send(Message message) {
		return send(message, SuccessHandler.nothing(), FailureHandler.log());
	}

	/**
	 * Sends a message. Does nothing on success.
	 * 
	 * @param message
	 *            the message
	 * @param failureHandler
	 *            the failure handler
	 */
	public Message send(Message message, FailureHandler<Message> failureHandler) {
		return send(message, SuccessHandler.nothing(), failureHandler);
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
	public Message send(Message message, SuccessHandler<Message> successHandler,
			FailureHandler<Message> failureHandler) {
		GWT.log("sending: " + message.getId());
		return send(Arrays.<Message> asList(message), successHandler, failureHandler).iterator().next();
	}

	/**
	 * Sends messages. Does nothing on success. Logs failures.
	 * 
	 * @param messages
	 *            the messages
	 */
	public List<Message> send(List<Message> messages) {
		return send(messages, SuccessHandler.nothing(), FailureHandler.log());
	}

	/**
	 * Sends messages. Will retry {@link retryCount} times after failure.
	 * 
	 * @param messages
	 *            the messages
	 * @param retryCount
	 *            the retry count after failure
	 */
	public List<Message> send(List<Message> messages, int retryCount) {
		if (retryCount == 0) {
			return send(messages);
		} else {
			return send(messages, SuccessHandler.nothing(), (t, m) -> send(messages, retryCount - 1));
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
	public List<Message> send(List<Message> messages, SuccessHandler<Message> successHandler,
			FailureHandler<Message> failureHandler) {
		List<Message> messagesToSend = new ArrayList<>();
		messagesToSend.addAll(outgoingMessageQueue);
		outgoingMessageQueue.clear();
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
		return messages;
	}

	public void get(String messageId, SuccessHandler<Message> successHandler, FailureHandler<Message> failureHandler) {
		restMessageService.get(messageId, new MethodCallback<Message>() {
			@Override
			public void onFailure(Method method, Throwable exception) {
				failureHandler.handle(exception, null);
			}

			@Override
			public void onSuccess(Method method, Message response) {
				successHandler.handle(response);
			}
		});
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
