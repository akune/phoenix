package de.kune.phoenix.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

public class Label extends Widget {

	public Label() {
		setElement(Document.get().createLabelElement());
	}

	public void setFor(Widget forWidget) {
		if (forWidget != null && forWidget.getElement() != null) {
			if (forWidget.getElement().getId() == null || forWidget.getElement().getId().isEmpty()) {
				forWidget.getElement().setId(DOM.createUniqueId());
			}
		}
		getElement().setAttribute("for", forWidget.getElement().getId());
	}

	public void setText(String text) {
		getElement().setInnerText(text);
	}
	
}
