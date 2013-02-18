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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
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
	private Handler autoplayHandler;
	private SlidesInfo slidesInfo;
	private View progressBar;


	protected int countPhotos;
	protected String projectId;
	private int count = 0;

	public interface PhotoPagerListener {
		void onClickItem(int photoId);
	}

	public ImagePager(Context context, String basePath, boolean transition, float viewWidth) {
		super(context);
		this.basePath = basePath;
		this.transition = transition;
		this.viewWidth = viewWidth;
		this.context = context;
		init();
	}

	public void setTitle(final String titleTable, final String titleUrl){
		titleView.setText(titleTable);
	}

	public int getCount(){
		return imageAdapter.getCount();
	}
	
	public void setCurrentPosition(int position,boolean smoothScroll){
		if (position >= getCount()) {
			position = getCount() - 1;
		}
		if (position < 0) {
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

	private void init() {
		slidesInfo = new SlidesInfo(basePath);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.image_pager, this, true);
		progressBar = findViewById(R.id.image_pager_progress);
		if (slidesInfo.count > 1) {
			initGallery();
		} else {
			initSingleImage();
		}
		Log.d(TAG, "Init: " + slidesInfo + ", transit = " + transition);
	}
	private void initGallery() {
		if(transition){
			viewPager = new ViewPager(getContext());
		} else {
			viewPager = new ViewPager(getContext()){
				float x1 = 0, x2, y1 = 0, y2, dx, dy;
				@Override
				public boolean onTouchEvent(MotionEvent event) {
					Log.d(TAG, "viewWidth = " + viewWidth);
					switch (event.getAction()) {
					case (MotionEvent.ACTION_DOWN):
						x1 = event.getX();
						y1 = event.getY();
						break;
					case (MotionEvent.ACTION_UP): {
						x2 = event.getX();
						y2 = event.getY();
						dx = x2 - x1;
						dy = y2 - y1;
						if (Math.abs(dx) > Math.abs(dy)) {
							float move = Math.abs(dx);
							Log.d(TAG, "move = " + move);
							if (dx > 0) {
								// "right";
								if (move < (viewWidth / 3)) {
									setCurrentPosition(getCurrentPosition() - 1, transition);
								} else {
									flipSlides(dx);
								}
							} else {
								// "left";
								if (move < (viewWidth / 3)) {
									setCurrentPosition(getCurrentPosition() + 1, transition);
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
		jumpTo(0);
		addView(viewPager);
	}

	private void initSingleImage() {
		ViewStub viewStub = (ViewStub) findViewById(R.id.image_pager_image_stub);
		final View view = viewStub.inflate();
		final ImageView imageView = (ImageView) view.findViewById(R.id.slideshow_item_image);
		new GetBitmapAsyncTask(){
			@Override
			protected void onPostExecute(Bitmap bmp) {
				if (isCancelled()) {
					return;
				}
				view.setBackgroundColor(backgroungColor);
				imageView.setImageBitmap(bmp);
				super.onPostExecute(bmp);
			}
		}.execute(slidesInfo.getFullPathToImage(1));
	}

	public void setViewWidth(int viewWidth){
		this.viewWidth = viewWidth;
	}

	private void flipSlides(final float dx){
		count = 0;
		final int mAutoplayDelay = 200;
		autoplayHandler = new Handler();
		autoplayHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				count++;
				int item = 0;
				if (dx > 0) {
					item = getCurrentPosition()-1;
				} else {
					item = getCurrentPosition()+1;
				}
				setCurrentPosition(item,transition);
				if(count < imageAdapter.getSlideCount()){
					autoplayHandler.postDelayed(this, mAutoplayDelay);
				}
		}}, mAutoplayDelay);
	}
	
	protected PagerAdapter getAdapter() {
		if (null == imageAdapter) {
			imageAdapter = new SimpleImageAdapter(context, slidesInfo);
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


	private class SlidesInfo {
		public final String assetDir;
		public final int count;
		public final String preffix;
		public final String suffix;

		public SlidesInfo(String basePath) {
			File file = new File(basePath);
			this.assetDir = file.getParent();
			String fileName = file.getName();
			this.count = Integer.valueOf(fileName.split("_")[1].split("\\.")[0]);
			this.preffix = fileName.split("_")[0];
			this.suffix = fileName.split("_")[1].split("\\.")[1];
		}

		public String getFullPathToImage(int position) {
			return this.assetDir + "/" + this.preffix + "_" + String.valueOf(position) + "." + this.suffix;
		}

		@Override
		public String toString() {
			return "SlidesInfo ["
					+ "assetDir=" + assetDir 
					+ ", count=" + count
					+ ", preffix=" + preffix 
					+ ", suffix=" + suffix 
					+ "]";
		}
		
	}

	private class GetBitmapAsyncTask extends AsyncTask<String, Void, Bitmap> {
		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(String... paths) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1;
			return BitmapFactory.decodeFile(paths[0], options);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			progressBar.setVisibility(View.INVISIBLE);
		}
		
	}

	protected class SimpleImageAdapter extends PagerAdapter{
		
		protected Context context;

		protected int imageViewId;

		private LayoutInflater inflater;
		private final SlidesInfo slidesInfo;
		
		public SimpleImageAdapter(Context context, SlidesInfo slidesInfo) {
			this.context = context;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.slidesInfo = slidesInfo;
		}

		@Override
		public int getCount() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public String getItem(int position) {
			return slidesInfo.getFullPathToImage(position + 1);
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			String path = getItem(position % slidesInfo.count);
			
			final View view = inflater.inflate(R.layout.slideshow_item_layout, null);
			view.setTag(position);
			
			final ImageView img = (ImageView)view.findViewById(R.id.slideshow_item_image);
			final FrameLayout background = (FrameLayout)view.findViewById(R.id.slide_show_frame);

			new GetBitmapAsyncTask(){
				@Override
				protected void onPostExecute(Bitmap bmp) {
					if (isCancelled()) {
						return;
					}
					background.setBackgroundColor(backgroungColor);
					img.setImageBitmap(bmp);
					super.onPostExecute(bmp);
				}
			}.execute(path);
			Log.d(TAG, "get " + position + " item from " + path);
			//
			container.addView(view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View)object);
		}

		public int getSlideCount(){
			return slidesInfo.count;
		}
	}

}
