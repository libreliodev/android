package com.librelio.notifications;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.librelio.activity.BillingActivity;
import com.librelio.activity.MainTabsActivity;
import com.niveales.wind.R;

public class GcmIntentService extends IntentService {
    private static final String WAURL = "waurl";
	private static final String CONTENT_AVAILABLE = "content-available";
	public static final int NOTIFICATION_ID = 1;
	private static final String TAG = "GcmIntentService";
	private static final String ALERT = "alert";
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                Log.i(TAG, "Received: " + extras.toString());
            	if (intent.hasExtra(ALERT)) {
            		String alertMessage = intent.getStringExtra(ALERT);
            		sendNotification(alertMessage);
            	} else if (intent.hasExtra(CONTENT_AVAILABLE)) {
            		if (intent.getStringExtra(CONTENT_AVAILABLE).equals("1")) {
            			String waurl = intent.getStringExtra(WAURL);
            			String title = intent.getStringExtra("title");
            			if (title == null || title.equals("")) {
            				title = getString(R.string.new_issue);
            			}
            			String subtitle = intent.getStringExtra("subtitle");
            			
            			// Start download if subscription valid
						BillingActivity
								.backgroundCheckForValidSubscriptionFailFast(
										getApplicationContext(), waurl, title,
										subtitle);

						NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
								this)
								.setSmallIcon(R.mipmap.ic_launcher)
								.setContentTitle(
										title + " "
												+ getString(R.string.available));
						// .setContentText("Click to read");

						// Create large icon from magazine cover png
						Resources res = getResources();
						int height = (int) res
								.getDimension(android.R.dimen.notification_large_icon_height);
						int width = (int) res
								.getDimension(android.R.dimen.notification_large_icon_width);
						// mBuilder.setLargeIcon(SystemHelper.decodeSampledBitmapFromFile(magazine.getPngPath(),
						// height, width));

						Intent resultIntent = new Intent(
								getApplicationContext(), MainTabsActivity.class);
						PendingIntent resultPendingIntent = PendingIntent
								.getActivity(this, 0, resultIntent,
										PendingIntent.FLAG_UPDATE_CURRENT);

						mBuilder.setContentIntent(resultPendingIntent);
						mBuilder.setAutoCancel(true);
						NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
						mNotificationManager.notify(waurl.hashCode(), mBuilder.build());
            		}
            	}
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainTabsActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}