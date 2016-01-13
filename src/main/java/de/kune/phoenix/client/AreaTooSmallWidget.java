package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AreaTooSmallWidget extends Composite {
	interface AreaTooSmallUiBinder extends UiBinder<Widget, AreaTooSmallWidget> {
	}

	private static AreaTooSmallUiBinder uiBinder = GWT.create(AreaTooSmallUiBinder.class);

	public AreaTooSmallWidget() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
