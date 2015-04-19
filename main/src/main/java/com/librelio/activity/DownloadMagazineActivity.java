//package com.librelio.activity;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.librelio.LibrelioApplication;
//import com.librelio.base.BaseActivity;
//import com.librelio.event.NewMagazineDownloadedEvent;
//import com.librelio.event.ReloadPlistEvent;
//import com.librelio.model.DownloadStatusCode;
//import com.librelio.model.dictitem.MagazineItem;
//import com.librelio.storage.DownloadsManager;
//import com.niveales.wind.R;
//import com.squareup.picasso.Picasso;
//
//import de.greenrobot.event.EventBus;
//
//public class DownloadMagazineActivity extends BaseActivity {
//
//    private MagazineItem magazine;
//    private ProgressBar progress;
//    private Handler handler = new Handler();
//
//    private Runnable loadPlistTask = new Runnable() {
//        @Override
//        public void run() {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    EventBus.getDefault().post(new ReloadPlistEvent());
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                        	int downloadStatus = magazine.getDownloadStatus();
//                            if (downloadStatus > DownloadStatusCode.QUEUED && downloadStatus < DownloadStatusCode.DOWNLOADED) {
//                                progress.setIndeterminate(false);
//                                progress.setProgress(downloadStatus);
//                                progressText.setText(R.string.download_in_progress);
//                            } else {
//                                progress.setIndeterminate(true);
//                                progressText.setText(getString(R.string.queued));
//                            }
//                            handler.postDelayed(loadPlistTask, 2000);
//                        }
//                    });
//                }
//            });
//        }
//    };
//	private TextView progressText;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_download_magazines);
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        String title = getIntent().getExtras().getString(BillingActivity.TITLE_KEY);
//        String subtitle = getIntent().getExtras().getString(BillingActivity.SUBTITLE_KEY);
//        String fileName = getIntent().getExtras().getString(BillingActivity.FILE_NAME_KEY);
//
//        magazine = new MagazineItem(this, title, subtitle, fileName);
//
//        ImageView preview = (ImageView) findViewById(R.id.download_preview_image);
//        progress = (ProgressBar)findViewById(R.id.download_progress);
//        progressText = (TextView)findViewById(R.id.download_progress_text);
//        Picasso.with(this).load(magazine.getPngUri()).fit().centerInside().into(preview);
//
//        ((TextView) findViewById(R.id.item_title)).setText(title);
//        ((TextView) findViewById(R.id.item_subtitle)).setText(subtitle);
//
//        Button cancelButton = (Button) findViewById(R.id.cancel_button);
//        cancelButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                magazine.clearMagazineDir(DownloadMagazineActivity.this);
//                DownloadsManager.removeDownload(DownloadMagazineActivity.this, magazine);
//                finish();
//            }
//        });
//    }
//
//    public void onEventMainThread(NewMagazineDownloadedEvent event) {
//        if (event.getMagazine().getFilePath().equals(magazine.getFilePath())) {
//            LibrelioApplication.startPDFActivity(this, magazine.getItemFilePath(), magazine.getTitle(), true);
//            finish();
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                finish();
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        handler.removeCallbacks(loadPlistTask);
//        handler.post(loadPlistTask);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        handler.removeCallbacks(loadPlistTask);
//    }
//}
