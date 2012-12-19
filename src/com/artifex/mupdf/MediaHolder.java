/**
 * 
 */
package com.artifex.mupdf;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;

import com.librelio.lib.adapter.SlideshowAdapter;

/**
 * @author Dmitry Valetin
 *
 */
public class MediaHolder extends FrameLayout implements Callback, OnBufferingUpdateListener, OnCompletionListener, OnVideoSizeChangedListener, OnPreparedListener {

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
	
	/**
	 * @param pContext
	 */
	@SuppressLint("SetJavaScriptEnabled")
	public MediaHolder(Context pContext, LinkInfo link, String basePath) throws IllegalStateException{
		super(pContext);
		mLinkInfo = link;
		uriString = link.uri;
		if(uriString == null)
			return;
		if(Uri.parse(uriString).getQueryParameter("warect") != null && Uri.parse(uriString).getQueryParameter("warect").equals("full")) {
			return;
		}
		
		boolean autoPlay = Uri.parse(uriString).getQueryParameter("waplay") != null && Uri.parse(uriString).getQueryParameter("waplay").equals("auto");

		
		if(uriString.startsWith("http://localhost/")) {
			// local resource
			final String path = Uri.parse(uriString).getPath();
			if (path.endsWith("jpg") || path.endsWith("png")
					|| path.endsWith("bmp")) {
				
				mAutoplayDelay = 800;
				if(Uri.parse(uriString).getQueryParameter("wadelay") != null) {
					mAutoplayDelay = Integer.valueOf(Uri.parse(uriString).getQueryParameter("wadelay"));
				}
				
				int bgColor = Color.BLACK;
				if(Uri.parse(uriString).getQueryParameter("wabgcolor") != null) {
					bgColor = Uri.parse(uriString).getQueryParameter("wabgcolor").equals("white") ?
							Color.WHITE : Color.BLACK;
				}
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				lp.gravity = Gravity.CENTER;
				mGallery = new SimpleGallery(getContext());

				mGallery.setAdapter(new SlideshowAdapter(getContext(),
						basePath
								+ Uri.parse(uriString).getPath()));
				mGallery.setLayoutParams(lp);
				mGallery.setBackgroundColor(bgColor);

				mGallery.setOnItemClickListener(new OnItemClickListener() {

					public void onClick(View pV) {
						if(listener != null) {
							listener.onClick(MediaHolder.this);
						}
					}

					@Override
					public void onItemClick(AdapterView<?> pParent, View pView,
							int pPosition, long pId) {
						if(listener != null) {
							listener.onClick(MediaHolder.this);
						}
					}
				});
				if(Uri.parse(uriString).getQueryParameter("watransition") != null && Uri.parse(uriString).getQueryParameter("watransition").equals("none")) {
					mGallery.setAnimation(null);
				}
				if(autoPlay) {
					mAutoplayHandler = new Handler();
					mAutoplayHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							int item = mGallery.getSelectedItemPosition() + 1;
							if(item >= mGallery.getCount()) {
								item = 0;
							}
							mGallery.setSelection(item);
							mAutoplayHandler.postDelayed(this, mAutoplayDelay);
						}}, mAutoplayDelay);
				} else {
					setVisibility(View.GONE);
				}
				addView(mGallery);
//				requestLayout();
			} else if (path.endsWith("mp4")) {
				
			} 
			
		} else if(uriString.contains("mp4")) {
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
