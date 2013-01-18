/**
 * 
 */
package com.artifex.mupdf;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.librelio.activity.SlideShowActivity;
import com.librelio.base.IBaseContext;
import com.librelio.task.CreateTempVideoTask;
import com.librelio.view.ImagePager;
import com.niveales.wind.R;

/**
 * The class for display all pdf's media-resources
 * 
 * @author Dmitry Valetin
 * @author Nikolay Moskvin <moskvin@netcook.org>
 */
public class MediaHolder extends FrameLayout implements Callback, OnBufferingUpdateListener, OnCompletionListener, OnVideoSizeChangedListener,
		OnPreparedListener {
	private static final String TAG = "MediaHolder";

	public static final String URI_STRING_KEY = "uri_string_key";
	public static final String BASE_PATH_KEY = "base_path_key";
	public static final String AUTO_PLAY_KEY = "auto_play_key";

	private LinkInfo linkInfo;

	private int autoPlayDelay;
	private Handler autoPlayHandler;

	private VideoView videoView;
	private WebView mWebView;
	private ImagePager imagePager;
	private MediaPlayer mediaPlayer;

	private String uriString;
	private OnClickListener listener = null;
	private SurfaceHolder holder;
	private String videoFileName;
	private boolean transition = true;

	public MediaHolder(Context context, LinkInfo linkInfo, String basePath) throws IllegalStateException{
		super(context);
		this.linkInfo = linkInfo;
		this.uriString = linkInfo.uri;
		

		if(uriString == null) {
			Log.w(TAG, "URI сan not be empty! basePath = " + basePath);
			return;
		}

		boolean fullScreen = linkInfo.isFullScreen();

		if (linkInfo.isExternal()) {
			if (linkInfo.isImageFormat()) {
				if (fullScreen) {
					onPlaySlideOutside(basePath);
				} else {
					onPlaySlideInside(basePath);
				}
			} else if (linkInfo.isVideoFormat()) {
				if (fullScreen) {
					onPlayVideoOutside(basePath);
				} else {
					onPlayVideoInside(basePath);
				}
			}
		} else if(linkInfo.hasVideoData()) {
			if (fullScreen) {
				onPlayVideoOutsideLocal();
			} else {
				onPlayVideoInsideLocal();
			}
		}
	}

	@Override
	public void onPrepared(MediaPlayer pMp) {}
	@Override
	public void onVideoSizeChanged(MediaPlayer pMp, int pWidth, int pHeight) {}
	@Override
	public void onCompletion(MediaPlayer pMp) {}
	@Override
	public void onBufferingUpdate(MediaPlayer pMp, int pPercent) {}
	@Override
	public void surfaceChanged(SurfaceHolder pHolder, int pFormat, int pWidth, int pHeight) {}
	@Override
	public void surfaceDestroyed(SurfaceHolder pHolder) {}

	public void recycle() {
		Log.d(TAG,"resycle was called");
		if(autoPlayHandler!=null){
			Log.d(TAG,"removeCallbacksAndMessages");
			autoPlayHandler.removeCallbacksAndMessages(null);
		}
		if(videoView!=null){
			videoView.stopPlayback();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder pHolder) {
		playVideo(videoFileName);
	}

	public LinkInfo getLinkInfo() {
		return linkInfo;
	}

	public void setOnClickListener(OnClickListener l) {
		listener = l;
	}

	protected void onPlayVideoInside(String basePath) {
		Log.d(TAG, "onPlayVideoInside " + basePath + ", linkInfo = " + linkInfo);
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.video_activity_layout, this, true);
		videoView = (VideoView) findViewById(R.id.video_frame);

		final IBaseContext baseContext = ((IBaseContext)getContext());
		new CreateTempVideoTask(baseContext.getVideoTempPath(), basePath){
			@Override
			protected void onPostExecute(String videoPath) {
				if (isCancelled()) {
					return;
				}
				videoView.setVideoPath(videoPath);
				MediaController mc = new MediaController(getContext());
				mc.setAnchorView(videoView);
				mc.setMediaPlayer(videoView);
				videoView.setMediaController(mc);
				videoView.requestFocus();
				if (null != getContext()) {
					mc.show(4000);
					videoView.start();
				}
			}
		}.execute(uriString);

	}

	protected void onPlayVideoOutside(String basePath) {
		Log.d(TAG, "onPlayVideoOutside " + basePath + ", linkInfo = " + linkInfo);
		final IBaseContext baseContext = ((IBaseContext)getContext());
		new CreateTempVideoTask(baseContext.getVideoTempPath(), basePath){
			@Override
			protected void onPostExecute(String videoPath) {
				if (isCancelled()) {
					return;
				}
				Intent intent = new Intent(Intent.ACTION_VIEW);
				Uri data = Uri.parse(videoPath);
				intent.setDataAndType(data, "video/*");
				getContext().startActivity(intent);
				/*Intent intent = new Intent(getContext(), VideoActivity.class);
				intent.putExtra(URI_STRING_KEY, uriString);
				intent.putExtra(BASE_PATH_KEY, basePath);
				intent.putExtra(AUTO_PLAY_KEY, autoPlay);
				getContext().startActivity(intent);*/
			}
		}.execute(uriString);
	}

	protected void onPlaySlideOutside(String basePath) {
		Log.d(TAG, "onPlaySlideOutside " + basePath + ", linkInfo = " + linkInfo);
		Intent intent = new Intent(getContext(), SlideShowActivity.class);
		intent.putExtra(MuPDFPageView.PATH_KEY, basePath);
		intent.putExtra(MuPDFPageView.LINK_URI_KEY, uriString);
		getContext().startActivity(intent);
	}

	protected void onPlaySlideInside(String basePath) {
		Log.d(TAG, "onPlaySlideInside " + basePath + ", linkInfo = " + linkInfo);
		boolean autoPlay = linkInfo.isAutoPlay();
		autoPlayDelay = 2000;
		if(Uri.parse(uriString).getQueryParameter("wadelay") != null) {
			autoPlayDelay = Integer.valueOf(Uri.parse(uriString).getQueryParameter("wadelay"));
			autoPlay = true;
		}
		int bgColor = Color.BLACK;
		if(Uri.parse(uriString).getQueryParameter("wabgcolor") != null) {
			bgColor = Uri.parse(uriString).getQueryParameter("wabgcolor").equals("white") ?
					Color.WHITE : Color.BLACK;
		}
		
		if(Uri.parse(uriString).getQueryParameter("watransition") != null) {
			transition = !Uri.parse(uriString).getQueryParameter("watransition").equals("none");
			autoPlayDelay = 1000;
			Log.d(TAG,"transition = "+transition);
		}
		//
		final String fullPath = basePath + Uri.parse(uriString).getPath();
		Log.d(TAG, "exist file " + fullPath + "? " + new File(fullPath).exists());
		imagePager = new ImagePager(getContext(), fullPath, transition, (linkInfo.right - linkInfo.left));
		post(new Runnable() {
			@Override
			public void run() {
				imagePager.setViewWidth(getWidth());
			}
		});
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		imagePager.setLayoutParams(lp);
		
		if(autoPlay) {
			autoPlayHandler = new Handler();
			autoPlayHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "autoPlayHandler start");
					imagePager.setCurrentPosition(imagePager.getCurrentPosition() + 1, transition);
					autoPlayHandler.postDelayed(this, autoPlayDelay);
				}}, autoPlayDelay);
		} else {
			setVisibility(View.GONE);
		}
		
		imagePager.setBackgroundColor(bgColor);
		
		addView(imagePager);
		requestLayout();
	}

	protected void onPlayVideoOutsideLocal() {
		Log.d(TAG, "onPlayVideoOutsideLocal linkInfo = " + linkInfo);
		mediaPlayer = getMediaPlayer(uriString);
		videoFileName = uriString;
		SurfaceView sv = new SurfaceView(getContext());
		
		sv.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		holder = sv.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		addView(sv);
		mediaPlayer.setDisplay(sv.getHolder());
	}

	protected void onPlayVideoInsideLocal() {
		Log.d(TAG, "onPlayVideoInsideLocal linkInfo = " + linkInfo);
		mWebView = new WebView(getContext());
		String htmlString = "<html> <body> <embed src=\""+uriString+"\"; type=application/x-shockwave-flash width="+getWidth()+" height="+getHeight()+"> </embed> </body> </html>";
		mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setPluginsEnabled(true);
		mWebView.getSettings().setPluginState(PluginState.ON_DEMAND);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAppCacheEnabled(true);
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		mWebView.setWebChromeClient(new WebChromeClient() {
			   public void onProgressChanged(WebView view, int progress) {
			     // Activities and WebViews measure progress with different scales.
			     // The progress meter will automatically disappear when we reach 100%
//				     activity.setProgress(progress * 1000);
			   }
			 });
		mWebView.setWebViewClient(new VideoWebViewClient());

		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		mWebView.setLayoutParams(lp);
		addView(mWebView);
		if(linkInfo.isAutoPlay()) {
			mWebView.loadUrl(uriString);
//				mWebView.loadDataWithBaseURL(null,htmlString ,"text/html", "UTF-8",null);
			
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
	}

	private MediaPlayer geMediaPlayer(String uri, String basePath) {
		MediaPlayer mp = null;
		if(basePath == null) {
			try {
				mp = new MediaPlayer();
				mp.setDataSource(uri);
				mp.prepareAsync();
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "create media player failed", e);
			} catch (SecurityException e) {
				Log.e(TAG, "create media player failed", e);
			} catch (IllegalStateException e) {
				Log.e(TAG, "create media player failed", e);
			} catch (IOException e) {
				Log.e(TAG, "create media player failed", e);
			}
		}
		return mp;
	}

	private void playVideo(String MediaFilePath) {
		try {
			// Create a new media player and set the listeners
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(MediaFilePath);
			mediaPlayer.setDisplay(holder);
			mediaPlayer.prepare();
			mediaPlayer.setOnBufferingUpdateListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnVideoSizeChangedListener(this);
			// mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		} catch (Exception e) {
			Log.e(TAG, "playVideo failed", e);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param pUriString
	 * @return
	 */
	private MediaPlayer getMediaPlayer(String uriString) throws IllegalStateException {
		return geMediaPlayer(uriString, null);
	}

	private void hitLinkUri(String uri) {
		if(linkInfo.uri.equals(uri)) {
			// TODO: start playing link
			setVisibility(View.VISIBLE);
			if(mWebView != null) {
				mWebView.loadUrl(uriString);
			}
		}
	}

	private class VideoWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d(TAG, "shouldOverrideUrlLoading url= " + url);
			view.loadUrl(url);
			return true;
		}
	}
}

