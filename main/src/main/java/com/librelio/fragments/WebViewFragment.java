package com.librelio.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.niveales.wind.R;

/**
 * A fragment that displays a WebView.
 *
 * The WebView is automatically paused or resumed when the Fragment is paused or resumed.
 */
public class WebViewFragment extends Fragment {
	private WebView mWebView;
	private boolean mIsWebViewAvailable;
	private String url;

	private static final String URL = "url";
	private View progressBar;

	public WebViewFragment() {
	}

	public static Fragment newInstance(String url) {
		Fragment f = new WebViewFragment();
		Bundle a = new Bundle();
		a.putString(URL, url);
		f.setArguments(a);
		return f;
	}

	/**
	 * Called to instantiate the view. Creates and returns the WebView.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (mWebView != null) {
			mWebView.destroy();
		}

		url = getArguments().getString(URL);
		View view = inflater.inflate(R.layout.fragment_web_view, container,
				false);

		mWebView = (WebView) view.findViewById(R.id.web_view);
		progressBar = view.findViewById(R.id.progress_bar);
		mIsWebViewAvailable = true;
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mWebView.getSettings().setDisplayZoomControls(true);
		}
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				// Check webview still exists
				if (view != null) {
					progressBar.setVisibility(View.GONE);
					mWebView.setVisibility(View.VISIBLE);
				}
				// ((ViewAnimator)getView().findViewById(R.id.view_animator)).setDisplayedChild(1);
				// mWebView.setWebViewClient(null);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				// ((ViewAnimator)getView().findViewById(R.id.view_animator)).setDisplayedChild(0);
				return false;
			}
		});
		mWebView.loadUrl(url);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	/**
	 * Called when the fragment is visible to the user and actively running.
	 * Resumes the WebView.
	 */
	@Override
	public void onPause() {
		super.onPause();
		mWebView.onPause();
	}

	/**
	 * Called when the fragment is no longer resumed. Pauses the WebView.
	 */
	@Override
	public void onResume() {
		mWebView.onResume();
		super.onResume();
	}

	/**
	 * Called when the WebView has been detached from the fragment. The WebView
	 * is no longer available after this time.
	 */
	@Override
	public void onDestroyView() {
		mIsWebViewAvailable = false;
		super.onDestroyView();
	}

	/**
	 * Called when the fragment is no longer in use. Destroys the internal state
	 * of the WebView.
	 */
	@Override
	public void onDestroy() {
		if (mWebView != null) {
			mWebView.destroy();
			mWebView = null;
		}
		super.onDestroy();
	}

	/**
	 * Gets the WebView.
	 */
	public WebView getWebView() {
		return mIsWebViewAvailable ? mWebView : null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_web_view, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.options_menu_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
