/**
 * 
 */
package com.librelio.library.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

import com.librelio.library.utils.db.DBHelper;
import com.niveales.testskis.R;

/**
 * @author Dmitry Valetin
 *
 */
public class SplashScreenActivity extends Activity {
	private Runnable delayRunnable = new Runnable() {
		@Override
		public void run() {
//			startActivity(new Intent(SplashScreenActivity.this, LibraryActivity.class));
			finish();
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Configuration newConfig = getResources().getConfiguration();
//		if ((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_XLARGE) == 0)
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//		setContentView(R.layout.splashscreen_layout);
//		Handler h = new Handler();
//		h.postDelayed(delayRunnable, 3000);
//		try {
//			DBHelper helper = new DBHelper(this, NivealesApplication.DB_FILE_NAME, "");
//			helper.open();
//			helper.close();
//		} catch (IllegalStateException e) {
//			h.removeCallbacks(delayRunnable);
//			AlertDialog.Builder b = new AlertDialog.Builder(this);
//			b.setMessage(e.getMessage()).setPositiveButton("Quit", new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface pDialog, int pWhich) {
//					finish();
//				}
//			});
//		}
	}
}
