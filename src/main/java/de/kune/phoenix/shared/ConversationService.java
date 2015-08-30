package de.kune.phoenix.shared;

import java.util.Set;

public interface ConversationService {
	
//	public static interface Participant {
//		String getId();
//		String getScreenName();
//		byte[] getPublicKey();
//	}
//	
//	public static interface Conversation {
//		String getId();
//		List<Participant> getParticipants();
//		List<Message> getAllMessages();
//		void sendMessage(String message);
//		List<Message> receiveNewMessages();
//	}
//	
//	void login(KeyPair keyPair);
//	List<Conversation> getConversations();
	
	Set<Message> receiveNewMessages();
	
}
