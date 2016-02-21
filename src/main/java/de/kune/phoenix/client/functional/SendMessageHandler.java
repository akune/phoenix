package de.kune.phoenix.client.functional;

import de.kune.phoenix.shared.Message;

@FunctionalInterface
public interface SendMessageHandler {

	Message sendMessage(String conversationId, String message);
	
}
