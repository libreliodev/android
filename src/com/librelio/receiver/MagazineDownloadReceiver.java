package com.librelio.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.librelio.activity.MainMagazineActivity;
import com.librelio.service.DownloadMagazineService;

public class MagazineDownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "MagazineDownloadReceiver";
    private DownloadManager mDManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
            Intent startMainMagazineActivityIntent = new Intent(context, MainMagazineActivity.class);
            startMainMagazineActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMainMagazineActivityIntent);
        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            Intent downloadCompleteIntent = new Intent(context, DownloadMagazineService.class);
            downloadCompleteIntent.putExtras(intent.getExtras());
            context.startService(downloadCompleteIntent);
        }
    }
}
