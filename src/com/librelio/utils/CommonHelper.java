package com.librelio.utils;

import java.util.List;

import android.os.AsyncTask;

/**
 *
 */
public class CommonHelper {

	/**
	 * Validate email string
	 * @param email is object of String
	 * @return true if valid email otherwise false
	 */
	public static boolean isEmail(String email) {
		String pattern = "^([a-zA-Z0-9_.-])+@([a-zA-Z0-9_.-])+\\.([a-zA-Z]){2,4}";
		return email != null && email.trim().length() > 0 && email.matches(pattern);
	}

	/**
	 * Validate String
	 * @param value is object of String
	 * @return true if value not empty otherwise false
	 */
	public static boolean isNotNull(String value) {
		return value != null && value.trim().length() > 0;
	}

	/**
	 * Validate StringBuilder
	 * @param value is object of String
	 * @return true if value not empty otherwise false
	 */
	public static boolean isNotNull(StringBuilder value) {
		return value != null && value.length() > 0;
	}

	/**
	 * Validate Array
	 * @param value the array of String
	 * @return true if value not empty otherwise false
	 */
	public static boolean isNotNull(Object[] collection) {
		return collection != null && collection.length > 0;
	}

	/**
	 * Validate Array
	 * @param value the array of Object
	 * @return true if value not empty otherwise false
	 */
	public static boolean isNotNull(List<?> collection) {
		return collection != null && collection.size() > 0;
	}

	/**
	 * Validate Array
	 * @param value the array of long
	 * @return true if value not empty otherwise false
	 */
	public static boolean isNotNull(long[] collection) {
		return collection != null && collection.length > 0;
	}

	/**
	 * Returns maximum value for two numbers
	 * @param value1 is first number
	 * @param value2 is second number
	 * @return value1 if it is greater than value2 
	 * or value2 if it is greater than the value1
	 */
	public static double maxValue(double value1, double value2) {
		return value1 > value2 ? value1: value2;
	}
	
	/**
	 * Cancels asynchronous tasks immediately.
	 * @param tasks array of asynchronous tasks
	 */
	public static void cancelTasks(AsyncTask<?, ?, ?>... tasks) {
		if (null != tasks) {
			for (AsyncTask<?, ?, ?> task : tasks) {
				if (null != task) task.cancel(true);
			}
		}
	}
}
