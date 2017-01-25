package de.kune.phoenix.client.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.google.gwt.user.client.Timer;

import de.kune.phoenix.client.functional.MessageHandler;
import de.kune.phoenix.shared.Message;
import de.kune.phoenix.shared.Messages;

public class MessageProcessor {

	public static MessageProcessor instance() {
		return instance;
	}

	private static final MessageProcessor instance = new MessageProcessor();

	private Map<Predicate<Message>, MessageHandler> messageHandlers = new LinkedHashMap<>();

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

	public void process(List<Message> messages) {
		ArrayList<Message> incomingMessageProcessingQueue = new ArrayList<>(messages);
		Collections.sort(incomingMessageProcessingQueue, Messages.SEQUENCE_KEY_ORDER);
		for (Message m : incomingMessageProcessingQueue) {
			handleReceivedMessage(m);
		}
	}

	private void handleReceivedMessage(final Message message) {
		final List<MessageHandler> handlers = new ArrayList<>();
		for (Entry<Predicate<Message>, MessageHandler> e : messageHandlers.entrySet()) {
			if (e.getKey().test(message)) {
				handlers.add(e.getValue());
			}
		}
		Runnable handler = () -> {
			for (MessageHandler h : handlers) {
				h.handleReceivedMessage(message, null);
			}
		};
		try {
			handler.run();
		} catch (IllegalStateException e) {
			new Timer() {
				@Override
				public void run() {
					handler.run();
				}
			}.schedule(0);
		}
	}

}
