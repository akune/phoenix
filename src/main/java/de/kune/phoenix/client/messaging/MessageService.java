package de.kune.phoenix.client.messaging;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.fusesource.restygwt.client.Defaults;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import de.kune.phoenix.shared.Message;

public class MessageService {

	public static class EventSource {

		public static enum State {
			CONNECTING, OPEN, CLOSED, UNKNOWN;
		}

		public static interface EventSourceHandler {
			void handleOpen(EventSource es);

			void handleClose(EventSource es);

			void handleMessage(EventSource es, String message);
			
			void handleError(EventSource es, String message);
		}

		public static EventSource connect(String uri, EventSourceHandler handler) {
			return new EventSource(uri, handler);
		}

		private EventSourceJso jso;
		
		private EventSource(String uri, EventSourceHandler handler) {
			jso = EventSourceJso.create(uri, this);
			jso.connect(handler);
		}

		public void close() {
			jso.close();
		}

		public State getState() {
			int state = jso.getState();
			if (state == 0) {
				return State.CONNECTING;
			}
			if (state == 1) {
				return State.OPEN;
			}
			if (state == 2) {
				return State.CLOSED;
			}
			return State.UNKNOWN;
		}

		private static class EventSourceJso extends JavaScriptObject {
			protected EventSourceJso() {
			}

			public native static final EventSourceJso create(String uri, EventSource eventSource) /*-{
				return { uri: uri, es: null, container: eventSource };
			}-*/;

			private native final void connect(EventSourceHandler handler) /*-{
			var es = new EventSource(this.uri);
			this.es = es;
			var container = this.container;
			this.es.addEventListener('message', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleMessage(Lde/kune/phoenix/client/messaging/MessageService$EventSource;Ljava/lang/String;)(container,evt.data);
			});
			this.es.addEventListener('open', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleOpen(Lde/kune/phoenix/client/messaging/MessageService$EventSource;)(container);
			});
			this.es.addEventListener('close', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleClose(Lde/kune/phoenix/client/messaging/MessageService$EventSource;)(container);
			});
			this.es.addEventListener('error', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleError(Lde/kune/phoenix/client/messaging/MessageService$EventSource;Ljava/lang/String;)(container,evt.data);
			});
		}-*/;

		private native final int getState() /*-{
			return this.es.readyState;
		}-*/;

		private native final void close() /*-{
			this.es.close();
		}-*/;

		
		}

	}

	public static class WebSocket {

		public static enum State {
			NOT_CONNECTED, CONNECTED, CONNECTING, CLOSED, UNKNOWN;
		}

		public static interface WebSocketHandler {
			void handleOpen(WebSocket ws);

			void handleClose(WebSocket ws);

			void handleMessage(WebSocket ws, String message);

			void handleError(WebSocket ws, String message);
		}

		public static WebSocket connect(String uri, WebSocketHandler handler) {
			return new WebSocket(uri, handler);
		}

		private WebSocketJso jso;

		private WebSocket(String uri, WebSocketHandler handler) {
			this.jso = WebSocketJso.create(uri, this);
			jso.connect(handler);
		}

		public void send(String message) {
			jso.send(message);
		}

		public void close() {
			jso.close();
		}

		public State getState() {
			int state = jso.getState();
			if (state == 0) {
				return State.NOT_CONNECTED;
			}
			if (state == 1) {
				return State.CONNECTED;
			}
			if (state == 2) {
				return State.CONNECTING;
			}
			if (state == 3) {
				return State.CLOSED;
			}
			return State.UNKNOWN;
		}

		private static class WebSocketJso extends JavaScriptObject {
			protected WebSocketJso() {
			}

			public native static final WebSocketJso create(String uri, WebSocket webSocket) /*-{
				return { uri: uri, ws: null, container: webSocket };
			}-*/;

			private native final void connect(WebSocketHandler handler) /*-{
				var ws = new WebSocket(this.uri);
				this.ws = ws;
				var container = this.container;
				this.ws.onopen = function(evt) {
					handler.@de.kune.phoenix.client.messaging.MessageService.WebSocket.WebSocketHandler::handleOpen(Lde/kune/phoenix/client/messaging/MessageService$WebSocket;)(container);
				};
				this.ws.onclose = function(evt) {
					handler.@de.kune.phoenix.client.messaging.MessageService.WebSocket.WebSocketHandler::handleClose(Lde/kune/phoenix/client/messaging/MessageService$WebSocket;)(container);
				};
				this.ws.onmessage = function(evt) {
					handler.@de.kune.phoenix.client.messaging.MessageService.WebSocket.WebSocketHandler::handleMessage(Lde/kune/phoenix/client/messaging/MessageService$WebSocket;Ljava/lang/String;)(container,evt.data);
				};
				this.ws.onerror = function(evt) {
					handler.@de.kune.phoenix.client.messaging.MessageService.WebSocket.WebSocketHandler::handleError(Lde/kune/phoenix/client/messaging/MessageService$WebSocket;Ljava/lang/String;)(container,evt.data);
				};
			}-*/;

			private native final int getState() /*-{
				return this.ws.readyState;
			}-*/;

			private native final void close() /*-{
				this.ws.close();
			}-*/;

			private native final void send(String message) /*-{
				this.ws.send(message);
			}-*/;
		}
	}

	@Path("/")
	public static interface RestMessageService extends RestService {
		@POST
		@Path("message")
		@Consumes(MediaType.APPLICATION_JSON)
		void post(Message message, MethodCallback<Void> callback);

		@POST
		@Path("conversation/{conversation}/message")
		@Consumes(MediaType.APPLICATION_JSON)
		void postToConversation(@PathParam("conversation") String conversationId, Message message,
				MethodCallback<Void> callback);

		@GET
		@Path("message")
		@Produces(MediaType.APPLICATION_JSON)
		void get(@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
				@QueryParam("recipient-id") String recipientId, MethodCallback<List<Message>> callback);

		@GET
		@Path("conversation/{conversation}/message")
		@Produces(MediaType.APPLICATION_JSON)
		void getFromConversation(@PathParam("conversation") String conversationId, @QueryParam("wait") boolean wait,
				@QueryParam("transmitted-after") Long transmittedAfter, @QueryParam("recipient-id") String recipientId,
				MethodCallback<List<Message>> callback);

	}

	private static final MessageService instance = new MessageService();

	public static MessageService instance() {
		return instance;
	}

	private final RestMessageService restMessageService;

	private MessageService() {
		Defaults.setServiceRoot(com.google.gwt.core.client.GWT.getModuleBaseURL()
				.replace(com.google.gwt.core.client.GWT.getModuleName() + "/", "") + "api");
		Defaults.setDateFormat(null);
		restMessageService = GWT.create(RestMessageService.class);
	}

	public void post(Message message, MethodCallback<Void> callback) {
		restMessageService.post(message, callback);
	}

	public void postToConversation(String conversationId, Message message, MethodCallback<Void> callback) {
		restMessageService.postToConversation(conversationId, message, callback);
	}

	public void get(boolean wait, Long transmittedAfter, String recipientId, MethodCallback<List<Message>> callback) {
		restMessageService.get(wait, transmittedAfter, recipientId, callback);
	}

	public void getFromConversation(String conversationId, boolean wait, Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId, MethodCallback<List<Message>> callback) {
		restMessageService.getFromConversation(conversationId, wait, transmittedAfter, recipientId, callback);
	}

}
