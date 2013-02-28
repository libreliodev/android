package com.librelio.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

public class WebAdvertisingActivity extends BaseActivity {

	public static final String PARAM_LINK = "PARAM_LINK";
	private String advertisingLink;
	
	private WebView webView;
	private Button doneButton;
	private Button browserButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_advertising);
		overridePendingTransition(R.anim.flip_right_in, R.anim.flip_left_out);
		
		advertisingLink = getIntent().getStringExtra(PARAM_LINK);
		
		webView = (WebView) findViewById(R.id.activity_web_advertising_browser_view);
		doneButton = (Button) findViewById(R.id.activity_web_advertising_button_done);
		browserButton = (Button) findViewById(R.id.activity_web_advertising_button_browser);
		
		prepareBarButtons();
		loadWebContent();
	}
	
	private void prepareBarButtons(){
		
		doneButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(self(), MainMagazineActivity.class);
				startActivity(intent);
			}
		});
		
		browserButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(
						Intent.ACTION_VIEW, Uri.parse(advertisingLink));
				startActivity(browserIntent);
			}
		});
	}
	
	private void loadWebContent(){
		if (advertisingLink != null){
			webView.getSettings().setJavaScriptEnabled(true);
			webView.setWebViewClient(new WebViewClient());
			webView.loadUrl(advertisingLink);
		}
	}
	
	private WebAdvertisingActivity self(){
		return this;
	}

}
