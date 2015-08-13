package de.kune.phoenix.client;

import java.util.Arrays;

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
	
    /**
     * Asserts that two Strings are not equal.
     */
    static public void assertNotEquals(String message, String expected, String actual) {
        if (expected != actual) {
            return;
        }
        if (expected != null && !expected.equals(actual)) {
            return;
        }
        String cleanMessage = message == null ? "" : message;
        fail(cleanMessage + " - expected: <" + expected + ">, actual: <" + actual + ">");
    }

    /**
     * Asserts that two Strings are not equal.
     */
    static public void assertNotEquals(String expected, String actual) {
        assertNotEquals(null, expected, actual);
    }

    public void assertEquals(String message, byte[] expected, byte[] actual) {
		assertEquals(message, Arrays.toString(expected), Arrays.toString(actual));
	}

	public void assertEquals(byte[] expected, byte[] actual) {
		assertEquals(null, expected, actual);
	}
	
	public void assertNotEquals(String message, byte[] expected, byte[] actual) {
		assertNotEquals(message, Arrays.toString(expected), Arrays.toString(actual));
	}
	
	public void assertNotEquals(byte[] expected, byte[] actual) {
		assertNotEquals(null, expected, actual);
	}
	
}
