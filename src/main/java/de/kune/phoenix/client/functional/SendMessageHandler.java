package de.kune.phoenix.client.functional;

@FunctionalInterface
public interface SendMessageHandler {

	void sendMessage(String conversationId, String message);
	
}
