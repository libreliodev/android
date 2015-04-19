/**
 * 
 */
package com.artifex.mupdfdemo;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.librelio.activity.SlideShowActivity;
import com.librelio.activity.VideoActivity;
import com.librelio.view.ImageLayout;
import com.niveales.wind.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
	public static final String TRANSITION_KEY = "transition_key";
	public static final String BG_COLOR_KEY = "bg_color_key";
	public static final String FULL_PATH_KEY = "full_path_key";
	public static final String PLAY_DELAY_KEY = "play_delay_key";
	public static final String PLAYBACK_POSITION_KEY = "playback_position_key";
	public static final String VIDEO_PATH_KEY = "video_path_key";
	public static final String INITIAL_SLIDE_POSITION = "initial_slide_position";
	
	private Context context;
	private String basePath;
	private LinkInfoExternal linkInfo;
	private Handler autoPlayHandler;
	private GestureDetector gestureDetector;
	private boolean autoPlayFlagMP = true;
	private ProgressDialog dialog;
	
	private SurfaceView videoView;
    private TextView errorTextView;
	private WebView mWebView;
	private ImageLayout imageLayout;
	private MediaPlayer mediaPlayer;

	private String uriString;
	private String videFilePath;
	private OnClickListener listener = null;
	private SurfaceHolder holder;
	private String videoFileName;
	
	private int autoPlayDelay;
	private boolean transition = true;
	private boolean autoPlay;
	private int bgColor;
	private String fullPath;
	private int currentPosition = 0;
	
	private MediaPlayer mMediaPlayer;

    public MediaHolder(Context context, LinkInfoExternal linkInfo, String basePath) throws IllegalStateException{
		super(context);
		this.basePath = basePath;
		this.context = context;
		this.linkInfo = linkInfo;
		this.uriString = linkInfo.url;
		this.videFilePath = getPathFromLocalhost(basePath, uriString);
		
		gestureDetector = new GestureDetector(new GestureListener());
		mMediaPlayer = new MediaPlayer();
		
		if(uriString == null) {
			Log.w(TAG, "URI ??an not be empty! basePath = " + basePath);
			return;
		}

		boolean fullScreen = linkInfo.isFullScreen();

		if (linkInfo.isExternal()) {
			if (linkInfo.isImageFormat()) {
				autoPlay = linkInfo.isAutoPlay();
				autoPlayDelay = 2000;
				if(Uri.parse(uriString).getQueryParameter("wadelay") != null) {
					autoPlayDelay = Integer.valueOf(Uri.parse(uriString).getQueryParameter("wadelay"));
					autoPlay = true;
				}
				bgColor = Color.BLACK;
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
				fullPath = basePath + Uri.parse(uriString).getPath();
				Log.d(TAG, "exist file " + fullPath + "? " + new File(fullPath).exists());
				if (fullScreen) {
					onPlaySlideOutside(basePath);
				} else {
					onPlaySlideInside(basePath);
				}
			} else if (linkInfo.isVideoFormat()) {
				if (fullScreen) {
					onPlayVideoOutside(videFilePath);
				} else {
					onPlayVideoInside(basePath);
				}
			}
		} else if(linkInfo.hasVideoData() 
				|| linkInfo.isMediaURI()) {
			if (fullScreen) {
				onPlayVideoOutsideLocal();
			} else {
				onPlayVideoInsideLocal();
			}
		}
	}
	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
        	if (linkInfo.hasVideoData()){
        		if(mMediaPlayer.isPlaying()){					
        			mMediaPlayer.pause();
        		} else {
        			mMediaPlayer.start();
        		}
        	} 
        	return super.onSingleTapConfirmed(e);
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
        	if (linkInfo.hasVideoData()){
	        	showWaitDialog();
	        	currentPosition = mMediaPlayer.getCurrentPosition();
	        	onPlayVideoOutside(videFilePath);
	        	mMediaPlayer.release();
	        	autoPlayFlagMP = false;
	        	//initMediaPlayer(true);
        	}else if (linkInfo.isImageFormat()){
				if (linkInfo.isToggleFullscreenAllowed()) {
					onPlaySlideOutside(basePath,
							imageLayout.getCurrentPosition());
				}
        	}
            return true;
        }
    }
	
	public void recycle() {
		Log.d(TAG,"resycle was called");
		if(autoPlayHandler!=null){
			Log.d(TAG,"removeCallbacksAndMessages");
			autoPlayHandler.removeCallbacksAndMessages(null);
		}
		if(mMediaPlayer!=null){
			mMediaPlayer.release();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder pHolder) {
		playVideo(videoFileName);
	}

	public LinkInfoExternal getLinkInfo() {
		return linkInfo;
	}

	public void setOnClickListener(OnClickListener l) {
		listener = l;
	}

	protected void onPlayVideoInside(String basePath) {
		showWaitDialog();
		Log.d(TAG, "onPlayVideoInside " + basePath + ", linkInfo = " + linkInfo);
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.activity_video, this, true);
        errorTextView = (TextView)findViewById(R.id.error_text);
		videoView = (SurfaceView)findViewById(R.id.surface_frame);
		videoView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});		
		holder = videoView.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.setKeepScreenOn(true);
		holder.addCallback(new SurfaceHolder.Callback() {
	        @Override
	        public void surfaceCreated(SurfaceHolder mHolder) {
	        	Log.d(TAG, "Callback.surfaceCreated");
	        	if(mMediaPlayer != null){
	        		mMediaPlayer.release();
	        	}
	        	mMediaPlayer = new MediaPlayer();
	            mMediaPlayer.setDisplay(mHolder);
	            initMediaPlayer();
	        }
	        @Override
	        public void surfaceDestroyed(SurfaceHolder holder) {
	        	Log.d(TAG, "Callback.surfaceDestroyed");
	        	cancelWaitDialog();
	        }
	        @Override
	        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }
	    });
		
	}

	private void initMediaPlayer(){
		
		Log.d(TAG, "path: " + videFilePath);
		File videoFile = new File(videFilePath);
        FileInputStream fis;
		try {
			fis = new FileInputStream(videoFile);
			mMediaPlayer.setDataSource(fis.getFD());
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    cancelWaitDialog();
                    if(autoPlayFlagMP){
                        mMediaPlayer.start();
                        setSurfaceViewScale();
                    }
                }
            });
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} catch (IOException e) {
			Log.e(TAG,"Problem with input stream!",e);
            videoView.setVisibility(View.GONE);
            errorTextView.setVisibility(View.VISIBLE);
		}
	}
	
	private void setSurfaceViewScale(){
		
		int width = videoView.getWidth();
		int height = videoView.getHeight();
		float boxWidth = width;
		float boxHeight = height;

		float videoWidth = mMediaPlayer.getVideoWidth();
		float videoHeight = mMediaPlayer.getVideoHeight();

		float wr = boxWidth / videoWidth;
		float hr = boxHeight / videoHeight;
		float ar = videoWidth / videoHeight;

		if (wr > hr)
		    width = (int) (boxHeight * ar);
		else
		    height = (int) (boxWidth / ar);
		
		holder.setFixedSize(width, height);
	}

	protected void onPlayVideoOutside(String path){
		Intent intent = new Intent(context, VideoActivity.class);
		intent.putExtra(VIDEO_PATH_KEY, path);
		intent.putExtra(PLAYBACK_POSITION_KEY, currentPosition);
		context.startActivity(intent);
	}
	
	protected void onPlaySlideOutside(String basePath) {
		onPlaySlideOutside(basePath, 0);
	}
	
	protected void onPlaySlideOutside(String basePath, int position) {
		Log.d(TAG, "onPlaySlideOutside " + basePath + ", linkInfo = " + linkInfo);
		Intent intent = new Intent(getContext(), SlideShowActivity.class);
		intent.putExtra(AUTO_PLAY_KEY, autoPlay);
		intent.putExtra(TRANSITION_KEY, transition);
		intent.putExtra(BG_COLOR_KEY,bgColor);
		intent.putExtra(PLAY_DELAY_KEY,autoPlayDelay);
		intent.putExtra(FULL_PATH_KEY,fullPath);
		intent.putExtra(INITIAL_SLIDE_POSITION, position);
		getContext().startActivity(intent);
	}

	protected void onPlaySlideInside(String basePath) {
		Log.d(TAG, "onPlaySlideInside " + basePath + ", linkInfo = " + linkInfo);
		
		imageLayout = new ImageLayout(getContext(), fullPath, transition);
		post(new Runnable() {
			@Override
			public void run() {
				imageLayout.setCurrentPosition(0, false);
			}
		});
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.CENTER;
		imageLayout.setLayoutParams(lp);
		
		imageLayout.setGestureDetector(gestureDetector);
		
		if(autoPlay) {
			autoPlayHandler = new Handler();
			autoPlayHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "autoPlayHandler start");
					imageLayout.setCurrentPosition(imageLayout.getCurrentPosition() + 1, transition);
					autoPlayHandler.postDelayed(this, autoPlayDelay);
				}}, autoPlayDelay);
		} else {
			setVisibility(View.GONE);
		}
		
		imageLayout.setBackgroundColor(bgColor);
		
		addView(imageLayout);
		requestLayout();
	}

	protected void onPlayVideoOutsideLocal() {
		Log.d(TAG, "onPlayVideoOutsideLocal linkInfo = " + linkInfo);
		videoFileName = uriString;
		SurfaceView sv = new SurfaceView(getContext());
		sv.setLayoutParams(new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		holder = sv.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		addView(sv);
		mediaPlayer.setDisplay(sv.getHolder());
		Log.d(TAG, "onPlayVideoOutsideLocal exit");
	}

	@SuppressLint("SetJavaScriptEnabled")
	protected void onPlayVideoInsideLocal() {
		Log.d(TAG, "onPlayVideoInsideLocal linkInfo = " + linkInfo);
		mWebView = new WebView(getContext());
		String htmlString = "<html> <body> <embed src=\""+uriString+"\"; type=application/x-shockwave-flash width="+getWidth()+" height="+getHeight()+"> </embed> </body> </html>";
		mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; U; Android 2.0; en-us; Droid Build/ESD20) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17");
		mWebView.getSettings().setJavaScriptEnabled(true);
//		mWebView.getSettings().setPluginsEnabled(true);
		mWebView.getSettings().setPluginState(PluginState.ON_DEMAND);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAppCacheEnabled(true);
		mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		mWebView.setWebChromeClient(new WebChromeClient() {
			   public void onProgressChanged(WebView view, int progress) {
			     // Activities and WebViews measure progress with different scales.
			     // The progress meter will automatically disappear when we reach 100%
				 //	activity.setProgress(progress * 1000);
			   }
			   
				@Override
				public void onShowCustomView(View view, CustomViewCallback callback) {
					super.onShowCustomView(view, callback);
				}

				@Override
				public void onHideCustomView(){
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
		Log.d(TAG, "onPlayVideoInsideLocal exit");

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
			mediaPlayer.prepareAsync();
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
		if(linkInfo.url.equals(uri)) {
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
	
	private String getPathFromLocalhost(String basePath, String uriString) {
		String local = "http://localhost/";
		int startIdx = local.length();
		int finIdx = uriString.length();
		if(uriString.contains("?")){
			finIdx = uriString.indexOf("?");
		}
		String assetsFile = uriString.substring(startIdx, finIdx);
		return basePath + "/" + assetsFile;
	}
	
	@Override
	public void onPrepared(MediaPlayer pmediaplayer) {}
	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {}
	@Override
	public void onCompletion(MediaPlayer pMp) {}
	@Override
	public void onBufferingUpdate(MediaPlayer pMp, int pPercent) {}
	@Override
	public void surfaceChanged(SurfaceHolder pHolder, int pFormat, int pWidth, int pHeight) {}
	@Override
	public void surfaceDestroyed(SurfaceHolder pHolder) {}
	
	private void showWaitDialog() {
		dialog = new ProgressDialog(getContext());
        dialog.setMessage(getResources().getString(R.string.loading));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
	}

	private void cancelWaitDialog() {
		if(dialog!=null){
			dialog.cancel();
			dialog = null;
		}
	}

}

