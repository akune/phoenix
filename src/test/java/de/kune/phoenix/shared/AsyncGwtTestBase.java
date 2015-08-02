package de.kune.phoenix.shared;

import com.google.gwt.core.client.Callback;
import com.google.gwt.junit.client.GWTTestCase;

public abstract class AsyncGwtTestBase extends GWTTestCase {
	protected class TestCallback<K, V> implements Callback<K, V> {
		private boolean finishTestOnSuccess;

		public TestCallback() {
			this(true);
		}

		public TestCallback(boolean finishTestOnSuccess) {
			this.finishTestOnSuccess = finishTestOnSuccess;
		}

		protected void handleSuccess(K result) {
		}

		protected void handleFailure(V reason) {
			fail(reason == null ? "unknown"
					: reason instanceof Throwable ? ((Throwable) reason).getMessage() : reason.toString());
		}

		@Override
		public final void onFailure(V reason) {
			try {
				handleFailure(reason);
			} catch (Throwable e) {
				reportUncaughtException(e);
			}
		}

		@Override
		public final void onSuccess(K result) {
			try {
				handleSuccess(result);
				if (finishTestOnSuccess) {
					finishTest();
				}
			} catch (Throwable e) {
				reportUncaughtException(e);
			}
		}

	}

}
