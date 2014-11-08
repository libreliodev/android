package com.librelio.products.ui.about;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.librelio.products.ui.BaseNivealesFragment;
import com.niveales.wind.R;

public class FacebookFragment extends BaseNivealesFragment {


	private WebView webView;
	ProgressDialog progress;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View rootView = inflater.inflate(R.layout.about_fragment_layout,  container, false);
		progress = new ProgressDialog(getActivity());
		webView = (WebView) rootView.findViewById(R.id.AboutDialogWebView);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				progress.dismiss();
			}
			
			@Override
			public void onPageStarted (WebView view, String url, Bitmap favicon) {
				progress.show();
			}
			@Override
	        public boolean shouldOverrideUrlLoading( WebView view, String url )
	        {
				
	            return false;
	        }
		});
		
		webView.loadUrl(getString(R.string.facebook_tab_url));
		webView.getSettings().setJavaScriptEnabled(true);
		
		return rootView;
	}
	
	@Override
	public boolean onBackPressed() {
		if(webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return false;
	}
}
