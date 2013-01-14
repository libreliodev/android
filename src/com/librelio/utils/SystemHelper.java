package com.librelio.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

public class SystemHelper {
	
	public static final String NULL_IMEI = "000000000000000";
	
	/**
	 * Gets emulator status
	 * @return true if emulator ran
	 * otherwise false
	 */
	public static boolean isEmulator(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String deviceId = telephonyManager.getDeviceId();
		if (deviceId == null) return true; 
		if (CommonHelper.isNotNull(deviceId)
				&& deviceId.equals(NULL_IMEI)) {
			return true;
		} else {
			return false;
		}
	}

}
