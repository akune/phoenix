package de.kune.phoenix.server;

import static de.kune.phoenix.server.util.ArrayUtils.contains;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.kune.phoenix.shared.Message;

public class EventSourceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Inject
	private ObjectStore<Message> messageStore;
	
	private static class MessageTransmitter implements Runnable {
		private ObjectMapper objectMapper = new ObjectMapper();
		private ObjectStore<Message> messageStore;
		private String recipientId;
		private String lastTransmission;
		private PrintWriter out;

		public MessageTransmitter(ObjectStore<Message> messageStore, String recipientId, String lastTransmission,
				PrintWriter out) {
			this.messageStore = messageStore;
			this.recipientId = recipientId;
			this.lastTransmission = lastTransmission;
			this.out = out;
		}

		@Override
		public void run() {
			while (!out.checkError()) {
				Set<Message> messages = messageStore.await(new Predicate<Message>() {
					@Override
					public boolean test(Message message) {
						return (message.getRecipientIds() == null || contains(message.getRecipientIds(), recipientId))
								&& (lastTransmission == null
										|| lastTransmission.compareTo(message.getTransmission()) < 0);
					}
				});
				for (Message message : messages) {
					if (lastTransmission == null || lastTransmission.compareTo(message.getTransmission()) < 0) {
						lastTransmission = message.getTransmission();
					}
				}
				try {
					String json = objectMapper.writeValueAsString(messages);
					json = "data: " + json.replace("\n", "\ndata: ");
					// out.write("event: " + message.getConversationId());
					out.write("data: " + json + "\n\n");
				} catch (JsonGenerationException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				out.flush();
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("---> request: " + req);
		resp.setContentType("text/event-stream");
		resp.setCharacterEncoding("UTF-8");

		final String recipientId = getRecipientId(req);
		System.out.println("---> recipient: " + recipientId);
		final String lastTransmission = getLastTransmission(req);
		System.out.println("---> lastTransmission: " + lastTransmission);
		new MessageTransmitter(messageStore, recipientId, lastTransmission, resp.getWriter()).run();
	}

	private String getLastTransmission(HttpServletRequest req) {
		return req.getParameter("last-transmission");
	}

	private static String getRecipientId(HttpServletRequest req) {
		String recipientId = req.getParameter("recipientId");
		if (recipientId != null) {
			try {
				recipientId = URLDecoder.decode(recipientId, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		return recipientId;
	}

}
