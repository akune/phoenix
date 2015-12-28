package de.kune.phoenix.client.messaging.communication;

import com.google.gwt.core.client.JavaScriptObject;

public class EventSource {

	public static enum State {
		CONNECTING, OPEN, CLOSED, UNKNOWN;
	}

	public static interface EventSourceHandler {
		void handleOpen(EventSource es);

		void handleClose(EventSource es);

		void handleMessage(EventSource es, String messageType, String message);

		void handleError(EventSource es, String messageType, String message);
	}

	public static EventSource connect(String uri, EventSource.EventSourceHandler handler) {
		return new EventSource(uri, handler);
	}

	private EventSource.EventSourceJso jso;

	private EventSource(String uri, final EventSource.EventSourceHandler handler) {
		jso = EventSourceJso.create(uri, this);
		jso.connect(new EventSourceHandler() {

			@Override
			public void handleOpen(EventSource es) {
				handler.handleOpen(es);
			}

			@Override
			public void handleClose(EventSource es) {
				handler.handleClose(es);
			}

			@Override
			public void handleMessage(EventSource es, String messageType, String message) {
				StringBuilder result = new StringBuilder();
				for (String line : message.split("\n")) {
					if (line.toLowerCase().startsWith("data:")) {
						line = line.substring(5).trim();
						result.append(line);
						result.append("\n");
					}
				}
				handler.handleMessage(es, messageType, result.toString());
			}

			@Override
			public void handleError(EventSource es, String messageType, String message) {
				handler.handleError(es, messageType, message);
			}

		});
	}

	public void subscribe(String messageType) {
		jso.subscribe(messageType);
	}

	public void unsubscribe(String messageType) {
		jso.unsubscribe(messageType);
	}

	public void close() {
		jso.close();
	}

	public EventSource.State getState() {
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

		public native static final EventSource.EventSourceJso create(String uri, EventSource eventSource) /*-{
			return { uri: uri, es: null, container: eventSource };
		}-*/;

		private native final void connect(EventSource.EventSourceHandler handler) /*-{
			var es = new EventSource(this.uri);
			var container = this.container;
			this.es = es;
			this.onMessage = function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleMessage(Lde/kune/phoenix/client/messaging/MessageService$EventSource;Ljava/lang/String;Ljava/lang/String;)(container,evt.event,evt.data);
			}; 
			this.es.addEventListener('message', this.onMessage);
			this.es.addEventListener('open', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleOpen(Lde/kune/phoenix/client/messaging/MessageService$EventSource;)(container);
			});
			this.es.addEventListener('close', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleClose(Lde/kune/phoenix/client/messaging/MessageService$EventSource;)(container);
			});
			this.es.addEventListener('error', function(evt) {
				handler.@de.kune.phoenix.client.messaging.MessageService.EventSource.EventSourceHandler::handleError(Lde/kune/phoenix/client/messaging/MessageService$EventSource;Ljava/lang/String;Ljava/lang/String;)(container,evt.event,evt.data);
			});
		}-*/;

		private native final int getState() /*-{
			return this.es.readyState;
		}-*/;

		private native final void close() /*-{
			this.es.close();
		}-*/;

		private native final void subscribe(String messageType) /*-{
			this.es.addEventListener(messageType, this.onMessage);
		}-*/;

		private native final void unsubscribe(String messageType) /*-{
			this.es.removeEventListener(messageType, this.onMessage);
		}-*/;

	}

}