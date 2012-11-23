package com.librelio.lib.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class CloudNotification extends Notification {


	public CloudNotification(int id, Context context, String title) {
		super();
		Intent intent = new Intent(context, context.getClass());
		contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
	}

}
