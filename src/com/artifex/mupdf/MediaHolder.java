/**
 * 
 */
package com.artifex.mupdf;

import java.io.IOException;

import android.annotation.SuppressLint;
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

import com.librelio.lib.ui.SlideShowActivity;
import com.librelio.lib.ui.VideoActivity;
import com.niveales.wind.R;

/**
 * @author Dmitry Valetin
 *
 */
public class MediaHolder extends FrameLayout implements Callback, OnBufferingUpdateListener, OnCompletionListener, OnVideoSizeChangedListener, OnPreparedListener {
	private static final String TAG = "MediaHolder";
	public static final String URI_STRING_KEY = "uri_string_key";
	public static final String BASE_PATH_KEY = "base_path_key";
	public static final String AUTO_PLAY_KEY = "auto_play_key";

	private LinkInfo mLinkInfo;
	private SimpleGallery mGallery = null;
	private WebView mWebVew = null;
	private float scale = 1.0f;
	private int mAutoplayDelay;
	private Handler mAutoplayHandler;
	private WebView mWebView;
	private String uriString;
	private OnClickListener listener = null;
	private MediaPlayer mMediaPlayer;
	private SurfaceHolder holder;
	private String videoFileName;
	private ImagePager imPager;
	private boolean transition = true;
	
	/**
	 * @param pContext
	 */
	@SuppressLint("SetJavaScriptEnabled")
	public MediaHolder(Context pContext, LinkInfo link,String basePath,boolean full) throws IllegalStateException{
		super(pContext);
		mLinkInfo = link;
		uriString = link.uri;
		Log.d(TAG,"basePath = "+basePath+"\nuriString = "+uriString);
		if(uriString == null)
			return;
		
		
		
		boolean autoPlay = Uri.parse(uriString).getQueryParameter("waplay") != null 
				&& Uri.parse(uriString).getQueryParameter("waplay").equals("auto");

		if(uriString.startsWith("http://localhost/")) {
			final String path = Uri.parse(uriString).getPath();
			if (path.endsWith("jpg") || path.endsWith("png") || path.endsWith("bmp")) {
				if(full){
					Log.d(TAG,"full = "+full);
					Intent intent = new Intent(getContext(),
							SlideShowActivity.class);
					intent.putExtra(MuPDFPageView.PATH_KEY, basePath);
					intent.putExtra(MuPDFPageView.LINK_URI_KEY, uriString);
					getContext().startActivity(intent);
					return;
				}
				mAutoplayDelay = 2000;
				if(Uri.parse(uriString).getQueryParameter("wadelay") != null) {
					mAutoplayDelay = Integer.valueOf(Uri.parse(uriString).getQueryParameter("wadelay"));
				}
				int bgColor = Color.BLACK;
				if(Uri.parse(uriString).getQueryParameter("wabgcolor") != null) {
					bgColor = Uri.parse(uriString).getQueryParameter("wabgcolor").equals("white") ?
							Color.WHITE : Color.BLACK;
				}
				
				if(Uri.parse(uriString).getQueryParameter("watransition") != null) {
					transition = !Uri.parse(uriString).getQueryParameter("watransition").equals("none");
					Log.d(TAG,"transition = "+transition);
				}
				//
				imPager = new ImagePager(getContext(),basePath + Uri.parse(uriString).getPath(),transition);
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				lp.gravity = Gravity.CENTER;
				imPager.setLayoutParams(lp);
				//
				if(autoPlay) {
					mAutoplayHandler = new Handler();
					mAutoplayHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							int item = imPager.getCurrentPosition()+1;
							imPager.setCurrentPosition(item,transition);
							if(item<imPager.getCount()-1){
								mAutoplayHandler.postDelayed(this, mAutoplayDelay);
							}
						}}, mAutoplayDelay);
				} else {
					setVisibility(View.GONE);
				}
				
				imPager.setBackgroundColor(bgColor);
				
				addView(imPager);
				requestLayout();
			} else if (path.endsWith("mp4")) {
				boolean fullVideo = Uri.parse(uriString).getQueryParameter("warect") != null 
						&& Uri.parse(uriString).getQueryParameter("warect").equals("full");
				if(fullVideo){
					Intent intent = new Intent(getContext(), VideoActivity.class);
					intent.putExtra(URI_STRING_KEY, uriString);
					intent.putExtra(BASE_PATH_KEY, basePath);
					intent.putExtra(AUTO_PLAY_KEY, autoPlay);
					getContext().startActivity(intent);
				} else {
					LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					inflater.inflate(R.layout.video_activity_layout, this, true);
					
					VideoActivity.createTempVideoFile(uriString, basePath, VideoActivity.getTempPath());
					VideoView video = (VideoView)findViewById(R.id.video_frame);// new VideoView(getContext());
					//video.setLayoutParams(new FrameLayout.LayoutParams
					//		(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT));
					video.setVideoPath(VideoActivity.getTempPath());
					MediaController mc = new MediaController(getContext());
					mc.setAnchorView(video);
					video.setMediaController(mc);
					video.requestFocus();
					//addView(video);
					if(autoPlay){
						video.start();
					}
				}
			}
		} else if(uriString.contains("mp4")) {
			boolean fullVideo = Uri.parse(uriString).getQueryParameter("warect") != null 
					&& Uri.parse(uriString).getQueryParameter("warect").equals("full");
			if(fullVideo){
				mMediaPlayer = getMediaPlayer(uriString);
				videoFileName = uriString;
				SurfaceView sv = new SurfaceView(getContext());
				
				sv.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				holder = sv.getHolder();
		        holder.addCallback(this);
		        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
				addView(sv);
				mMediaPlayer.setDisplay(sv.getHolder());
			} else {
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
				mWebView.setWebViewClient(new MyClient());
	
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				lp.gravity = Gravity.CENTER;
				mWebView.setLayoutParams(lp);
				addView(mWebView);
				if(autoPlay) {
					mWebView.loadUrl(uriString);
	//				mWebView.loadDataWithBaseURL(null,htmlString ,"text/html", "UTF-8",null);
					
					setVisibility(View.VISIBLE);
				} else {
					setVisibility(View.GONE);
				}
			}
			
		}
	}

	
	


	public void hitLinkUri(String uri) {
		if(mLinkInfo.uri.equals(uri)) {
			// TODO: start playing link
			setVisibility(View.VISIBLE);
			if(mWebView != null) {
				mWebView.loadUrl(uriString);
			}
		}
	}
	
	public LinkInfo getLinkInfo() {
		return mLinkInfo;
	}

	public void setOnClickListener(OnClickListener l) {
		listener = l;
	}

	/**
	 * @param pUriString
	 * @return
	 */
	private MediaPlayer getMediaPlayer(String uriString) throws IllegalStateException {
		return geMediaPlayer(uriString, null);
	}
	
	public MediaPlayer geMediaPlayer(String uri, String basePath) throws IllegalStateException {
		MediaPlayer mp = null;
		if(basePath == null) {
			try {
				mp = new MediaPlayer();
				mp.setDataSource(uri);
				mp.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return mp;
	}

	/**
	 * 
	 */
	public void clearResources() {
		// TODO Auto-generated method stub
		if(mGallery != null) {
//			mGallery.setAdapter(null);
			mGallery = null;
		}
	}





	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder pHolder) {
		 playVideo(videoFileName);
		
	}





	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
	 */
	@Override
	public void surfaceChanged(SurfaceHolder pHolder, int pFormat, int pWidth,
			int pHeight) {
		// TODO Auto-generated method stub
		
	}





	/* (non-Javadoc)
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder pHolder) {
		// TODO Auto-generated method stub
		
	}
	
	private void playVideo(String MediaFilePath) {
        try {
            // Create a new media player and set the listeners
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(MediaFilePath);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        } catch (Exception e) {
        	e.printStackTrace();
        	throw new IllegalStateException(e);
        }
    }





	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer pMp) {
		// TODO Auto-generated method stub
		
	}





	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnVideoSizeChangedListener#onVideoSizeChanged(android.media.MediaPlayer, int, int)
	 */
	@Override
	public void onVideoSizeChanged(MediaPlayer pMp, int pWidth, int pHeight) {
		// TODO Auto-generated method stub
		
	}





	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
	 */
	@Override
	public void onCompletion(MediaPlayer pMp) {
		// TODO Auto-generated method stub
		
	}





	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnBufferingUpdateListener#onBufferingUpdate(android.media.MediaPlayer, int)
	 */
	@Override
	public void onBufferingUpdate(MediaPlayer pMp, int pPercent) {
		// TODO Auto-generated method stub
		
	}

}

class MyClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.i("url loaded", "url::: " + url);
        view.loadUrl(url);
        return true;
    }
}
