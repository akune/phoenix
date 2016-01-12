package de.kune.phoenix.client;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.user.client.ui.Widget;

public final class Animations {
	private Animations() {
		// Do nothing.
	}
	
	public static Animation fadeIn(final Widget w) {
		return new Animation() {

			@Override
			protected void onComplete() {
				super.onComplete();
				w.setVisible(true);
				w.getElement().getStyle().setOpacity(1);
			}

			@Override
			protected void onStart() {
				super.onStart();
				w.removeStyleName("hidden");
				w.setVisible(false);
				w.getElement().getStyle().setOpacity(0);
			}

			@Override
			protected void onUpdate(double progress) {
				w.setVisible(true);
				w.getElement().getStyle().setOpacity(progress);
			}
		};
	}

	public static Animation fadeOut(final Widget w) {
		return new Animation() {
			
			@Override
			protected void onComplete() {
				super.onComplete();
				w.addStyleName("hidden");
				w.setVisible(false);
				w.getElement().getStyle().setOpacity(1);
			}
			
			@Override
			protected void onStart() {
				super.onStart();
				w.setVisible(true);
				w.getElement().getStyle().setOpacity(1);
			}
			
			@Override
			protected void onUpdate(double progress) {
				w.setVisible(true);
				w.getElement().getStyle().setOpacity(1.0d - progress);
			}
		};
	}
	


}
