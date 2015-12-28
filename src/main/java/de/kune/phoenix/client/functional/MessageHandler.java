package de.kune.phoenix.client.functional;

import de.kune.phoenix.shared.Message;

@FunctionalInterface
public interface MessageHandler {

	/**
	 * Handles a (possibly decrypted) message.
	 * 
	 * @param message
	 *            the message to handle
	 * @param content
	 *            the decrypted content or null
	 */
	void handleReceivedMessage(Message message, byte[] content);

}
