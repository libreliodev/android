package com.librelio.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

public class WebViewActivity extends BaseActivity {
	
	public static void startWithUrl(Context context, String url) {
		if (url.contains("youtube") || url.contains("dailymotion") || url.contains("vimeo")) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			context.startActivity(intent);
			return;
		}
		Intent webAdvertisingActivityIntent = new Intent(context, WebViewActivity.class);
		webAdvertisingActivityIntent.putExtra(WebViewActivity.PARAM_LINK, url);
		context.startActivity(webAdvertisingActivityIntent);
	}

	public static final String PARAM_LINK = "PARAM_LINK";
	private String advertisingLink;

	private WebView webView;
	private Button doneButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.activity_webview);

		overridePendingTransition(R.anim.flip_right_in, R.anim.flip_left_out);

		advertisingLink = getIntent().getStringExtra(PARAM_LINK);

		webView = (WebView) findViewById(R.id.activity_web_advertising_browser_view);
		doneButton = (Button) findViewById(R.id.activity_web_advertising_button_done);

		prepareBarButtons();
		loadWebContent();
	}

	private void prepareBarButtons() {

		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void loadWebContent() {
		if (advertisingLink != null) {
			webView.getSettings().setJavaScriptEnabled(true);
			webView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(WebView view, String url) {
					setProgressBarIndeterminateVisibility(false);
					super.onPageFinished(view, url);
				}
			});
			webView.loadUrl(advertisingLink);
			setProgressBarIndeterminateVisibility(true);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_web_view, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.options_menu_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
					.parse(advertisingLink));
			startActivity(browserIntent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
