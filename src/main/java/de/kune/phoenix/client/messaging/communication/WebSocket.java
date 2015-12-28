package de.kune.phoenix.client.messaging.communication;

import com.google.gwt.core.client.JavaScriptObject;

public class WebSocket {

	public static enum State {
		NOT_CONNECTED, CONNECTED, CONNECTING, CLOSED, UNKNOWN;
	}

	public static interface WebSocketHandler {
		void handleOpen(WebSocket ws);

		void handleClose(WebSocket ws);

		void handleMessage(WebSocket ws, String message);

		void handleError(WebSocket ws, String message);
	}

	public static WebSocket connect(String uri, WebSocket.WebSocketHandler handler) {
		return new WebSocket(uri, handler);
	}

	private WebSocket.WebSocketJso jso;

	private WebSocket(String uri, WebSocket.WebSocketHandler handler) {
		this.jso = WebSocketJso.create(uri, this);
		jso.connect(handler);
	}

	public void send(String message) {
		jso.send(message);
	}

	public void close() {
		jso.close();
	}

	public WebSocket.State getState() {
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

		public native static final WebSocket.WebSocketJso create(String uri, WebSocket webSocket) /*-{
			return { uri: uri, ws: null, container: webSocket };
		}-*/;

		private native final void connect(WebSocket.WebSocketHandler handler) /*-{
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