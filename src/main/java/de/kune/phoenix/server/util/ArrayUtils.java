package de.kune.phoenix.server.util;

public class ArrayUtils {

	public static boolean contains(String[] array, String element) {
		for (String item : array) {
			if (item.equals(element)) {
				return true;
			}
		}
		return false;
	}

}
