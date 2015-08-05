package de.kune.phoenix.shared;

import static com.google.gwt.core.client.ScriptInjector.TOP_WINDOW;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.ScriptInjector;

public class CipherSuite {

	private static final List<String> SCRIPT_FILES = Arrays.asList(new String[] { "{moduleBaseUrl}/all.js" });

	private static enum State {
		UNINITIALIZED, INITIALIZING, READY;
	}

	private static final CipherSuite instance = new CipherSuite();
	private static boolean scriptsLoaded;

	public static void init(Callback<Void, Exception> doneCallback) {
		if (instance.getState() == State.READY) {
			doneCallback.onSuccess(null);
		} else if (instance.getState() == State.UNINITIALIZED) {
			instance.doInit(doneCallback);
		} else {
			throw new IllegalStateException("illegal state " + instance.getState());
		}
	}
	
	public static CipherSuite getInstance() {
		return instance;
	}
	
	public static boolean isReady() {
		return instance.getState() == State.READY;
	}
	
	public static void assertReady() {
		if (!isReady()) {
			throw new IllegalStateException("cipher suite is not ready");
		}
	}

	private State state = State.UNINITIALIZED;

	protected CipherSuite() {
	}

	protected void doInit(final Callback<Void, Exception> callback) {
		if (state == State.UNINITIALIZED) {
			state = State.INITIALIZING;
			loadScripts(SCRIPT_FILES, new Callback<Void, Exception>() {
				@Override
				public void onFailure(Exception reason) {
					state = State.UNINITIALIZED;
					callback.onFailure(new RuntimeException("could not load script files", reason));
				}

				@Override
				public void onSuccess(Void result) {
					state = State.READY;
					callback.onSuccess(null);
				}
			});
		} else if (state != State.INITIALIZING) {
			callback.onSuccess(null);
		} else {
			callback.onFailure(new IllegalStateException("initialization in progress"));
		}
	}

	private static void loadScripts(final List<String> scripts, final Callback<Void, Exception> callback) {
		if (scriptsLoaded || scripts.isEmpty()) {
			scriptsLoaded = true;
			callback.onSuccess(null);
		} else {
			String script = scripts.iterator().next().replace("{moduleBaseUrl}", GWT.getModuleBaseURL());
			GWT.log("loading script " + script);
			ScriptInjector.fromUrl(script).setWindow(TOP_WINDOW).setRemoveTag(false)
					.setCallback(new Callback<Void, Exception>() {
						@Override
						public void onSuccess(Void result) {
							loadScripts(scripts.subList(1, scripts.size()), callback);
						}

						@Override
						public void onFailure(Exception reason) {
							callback.onFailure(reason);
						}
					}).inject();
		}
	}

	public State getState() {
		return state;
	}

}
