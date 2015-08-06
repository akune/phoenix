package de.kune.phoenix.shared;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;

public final class JsArrayUtil {

	private JsArrayUtil() {
		// Do nothing.
	}
	
	public static byte[] toByteArray(JsArrayNumber arrayNumber) {
		if (arrayNumber == null) {
			return null;
		}
		byte[] result = new byte[arrayNumber.length()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) arrayNumber.get(i);
		}
		return result;
	}
	
	public static JsArrayNumber toJsArrayNumber(byte[] byteArray) {
		if (byteArray == null) {
			return null;
		}
		JsArrayNumber jsArray = JavaScriptObject.createArray(byteArray.length).cast();
		for (int i = 0; i < byteArray.length; i++) {
			jsArray.set(i, byteArray[i] >= 0 ? byteArray[i] : byteArray[i] + 256);
		}
		return jsArray;
	}
	
}
