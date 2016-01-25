package de.kune.phoenix.client;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;

public class UpdateEvent extends GwtEvent<UpdateHandler> {

	public static final Type<UpdateHandler> TYPE = new Type<UpdateHandler>();
	private Widget source;
	
	public UpdateEvent(Widget source) {
		this.source = source;
	}
	
	public Widget getSource() {
		return source;
	}

	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<de.kune.phoenix.client.UpdateHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(de.kune.phoenix.client.UpdateHandler handler) {
		handler.onUpdate(this);
	}

}
