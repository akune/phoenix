package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AreaTooSmallWidget extends Composite {
	interface MyUiBinder extends UiBinder<Widget, AreaTooSmallWidget> {
	}

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

	public AreaTooSmallWidget() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
