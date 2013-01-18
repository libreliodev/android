package com.librelio.view;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.niveales.wind.R;

public class ImagePager extends RelativeLayout {

	protected static final String TAG = "ImagePager";

	protected static final int MULTIPLIER = 100000;
	protected ViewPager viewPager;
	protected TextView titleView;

	protected Context context;
	private LayoutInflater inflater;
	private String basePath;
	private int backgroungColor = Color.BLACK;
	private boolean transition = true;
	private float viewWidth;

	protected PhotoPagerListener listener;
	private SimpleImageAdapter imageAdapter;

	protected int countPhotos;
	protected String projectId;
	protected int minCountFromInfinityLoop = 12;
	private Handler mAutoplayHandler;
	private int count = 0;

	public interface PhotoPagerListener {
		void onClickItem(int photoId);
	}

	public ImagePager(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public ImagePager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ImagePager(Context context, String basePath, boolean transition, float viewWidth) {
		super(context);
		this.basePath = basePath;
		this.transition = transition;
		this.viewWidth = viewWidth;
		init(context);
	}

	public void setTitle(final String titleTable, final String titleUrl){
		titleView.setText(titleTable);
	}

	public void setMinCountFromInfinityLoop(int minCountFromInfinityLoop){
		this.minCountFromInfinityLoop = minCountFromInfinityLoop;
	}

	public int getCount(){
		return imageAdapter.getCount();
	}
	
	public void setCurrentPosition(int position,boolean smoothScroll){
		if(position>=getCount()){
			position = getCount()-1;
		}
		if(position<0){
			position = 0;
		}
		viewPager.setCurrentItem(position, smoothScroll);
	}

	public int getCurrentPosition(){
		return viewPager.getCurrentItem();
	}
	
	public void jumpTo(int index) {
		viewPager.setCurrentItem(getCount() * MULTIPLIER / 2 + index, false);
	}

	protected int getPageMargin() {
		return 0;
	}

	protected int getPageLimit() {
		return 2;
	}

	private void init(Context context) {
		this.context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.image_pager, this, true);

		if(transition){
			viewPager = new ViewPager(getContext());
		} else {
			viewPager = new ViewPager(getContext()){
				float x1 = 0, x2, y1 = 0, y2, dx, dy;
				@Override
			    public boolean onTouchEvent(MotionEvent event) {
					Log.d(TAG,"viewWidth = "+viewWidth);
					switch(event.getAction()) {
					    case(MotionEvent.ACTION_DOWN):
					        x1 = event.getX();
					        y1 = event.getY();
					        break;
					    case(MotionEvent.ACTION_UP): {
					        x2 = event.getX();
					        y2 = event.getY();
					        dx = x2-x1;
					        dy = y2-y1;
					        if(Math.abs(dx) > Math.abs(dy)) {
								float move = Math.abs(dx);
								Log.d(TAG,"move = "+move);
								if(dx>0){
					            	//"right";
									if(move<(viewWidth/3)){
										setCurrentPosition(getCurrentPosition()-1,transition);
									} else {
										flipSlides(dx);
									}
								} else {
									//"left";
									if(move<(viewWidth/3)){
										setCurrentPosition(getCurrentPosition()+1,transition);
									} else {
										flipSlides(dx);
									}
								}
					        }
					    }
					}
			        return true;
			    }
			};
		}
		viewPager.setAdapter(getAdapter());
		viewPager.setHorizontalFadingEdgeEnabled(true);
		viewPager.setFadingEdgeLength(0);
		viewPager.setOffscreenPageLimit(getPageLimit());
		viewPager.setCurrentItem(imageAdapter.getSlideCount()*MULTIPLIER, false);
		addView(viewPager);
	}

	
	public void setViewWidth(int viewWidth){
		this.viewWidth = viewWidth;
	}

	private void flipSlides(final float dx){
		count = 0;
		final int mAutoplayDelay = 200;
		mAutoplayHandler = new Handler();
		mAutoplayHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				count++;
				int item = 0;
				if(dx>0){
					item = getCurrentPosition()-1;
				} else {
					item = getCurrentPosition()+1;
				}
				setCurrentPosition(item,transition);
				if(count<imageAdapter.getSlideCount()){
					mAutoplayHandler.postDelayed(this, mAutoplayDelay);
				}
		}}, mAutoplayDelay);
	}
	
	protected PagerAdapter getAdapter() {
		if (null == imageAdapter) {
			imageAdapter = new SimpleImageAdapter(context, minCountFromInfinityLoop,basePath);
		}
		return imageAdapter;
	}

	public void setPhotoPagerListener(PhotoPagerListener listener) {
		this.listener = listener;
	}

	@Override
	public void setBackgroundColor(int color) {
		backgroungColor = color;
		super.setBackgroundColor(color);
	}
	
	protected class SimpleImageAdapter extends PagerAdapter{
		
		protected Context context;

		protected int imageViewId;

		protected int minCountFromInfinityLoop;
		private String mSlideshowAssetDir;
		private int mSlideshowCount;
		private String mSlideshowPreffix;
		private String mSlideshowSuffix;
		private LayoutInflater inflater;
		
		public SimpleImageAdapter(Context context, int minCountFromInfinityLoop, String path) {
			this.context = context;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			File file = new File(path);
			mSlideshowAssetDir = file.getParent();
			String fileName = file.getName();
			mSlideshowCount = Integer.valueOf(fileName.split("_")[1].split("\\.")[0]);
			mSlideshowPreffix = fileName.split("_")[0];
			mSlideshowSuffix = fileName.split("_")[1].split("\\.")[1];
			this.minCountFromInfinityLoop = minCountFromInfinityLoop;
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public String getItem(int pPosition) {
			return mSlideshowAssetDir + "/" + mSlideshowPreffix + "_"
					+ String.valueOf(pPosition + 1) + "." + mSlideshowSuffix;
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			Log.d(TAG, "instantiateItem");
			
			String path = getItem(position % mSlideshowCount);
			
			final View view = inflater.inflate(R.layout.slideshow_item_layout, null);
			view.setTag(position);
			//
			new AsyncTask<Object, Void, Bitmap>() {
				String path;
				ImageView img;
				FrameLayout background;
				@Override
				protected Bitmap doInBackground(Object... pParams) {
					path = (String) pParams[0];
					File file = new File(path);
					int size = (int)(file.length()/1024);
					img = (ImageView)view.findViewById(R.id.SlideshowImage);
					background = (FrameLayout)view.findViewById(R.id.slide_show_frame);
					BitmapFactory.Options options=new BitmapFactory.Options();
					if(size>200){
						options.inSampleSize = 2;
					} else {
						options.inSampleSize = 1;
					}
					return BitmapFactory.decodeFile(path,options);
				}
				
				@Override
				protected void onPostExecute(Bitmap bmp) {
					background.setBackgroundColor(backgroungColor);
					img.setImageBitmap(bmp);
				}
			}.execute(path);
			
			//
			container.addView(view);
			return view;
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
		}
		
		public int getSlideCount(){
			return mSlideshowCount;
		}
	}

}
