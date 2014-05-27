package com.librelio.view;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.librelio.utils.SystemHelper;
import com.niveales.wind.R;

public class ImageLayout extends RelativeLayout {

	protected static final String TAG = "ImageLayout";

	protected ViewPager viewPager;

	protected Context context;
	private LayoutInflater inflater;
	private int backgroundColor = Color.BLACK;
	private boolean transition = true;

	private SimpleImageAdapter imageAdapter;
	private SlidesInfo slidesInfo;
	private View progressBar;

	private int currentImageViewPosition;

	private ImageView flipBookImageView;
    private TextView flipBookText;

	public boolean getBitmapAsyncTaskCancelled;

	private GestureDetector gestureDetector;

    public ImageLayout(Context context, String basePath, boolean transition) {
		super(context);
		this.transition = transition;
		this.context = context;
		slidesInfo = new SlidesInfo(basePath);
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.image_pager, this, true);
		progressBar = findViewById(R.id.image_pager_progress);
		
		getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				init();
				getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
	}
	
	public void setGestureDetector(final GestureDetector gestureDetector) {
		if (gestureDetector != null) {
				this.gestureDetector = gestureDetector;
		}
	}

	public void setCurrentPosition(int position, boolean smoothScroll) {
		if (viewPager != null) {
			if (position >= slidesInfo.count) {
				position = slidesInfo.count - 1;
			}
			if (position < 0) {
				position = 0;
			}
			viewPager.setCurrentItem(position, smoothScroll);
		} else if (flipBookImageView != null) {
			if (position >= slidesInfo.count) {
				position = 0;
			}
			if (position < 0) {
				position = slidesInfo.count - 1;
			}
			setCurrentImageViewPosition(position);
		}
	}

	public int getCurrentPosition() {
		if (viewPager != null) {
			return viewPager.getCurrentItem();
		} else {
			return currentImageViewPosition;
		}
	}

	private void init() {
		if (slidesInfo.count > 1) {
			if (transition) {
				initSwipeGallery();
			} else {
				initFlipBookGallery();
			}
		} else {
			initSingleImage();
		}
	}

	private void initSwipeGallery() {
			viewPager = new ViewPager(getContext());
			imageAdapter = new SimpleImageAdapter(context, slidesInfo);
			viewPager.setAdapter(imageAdapter);
			viewPager.setHorizontalFadingEdgeEnabled(true);
			viewPager.setFadingEdgeLength(0);
			viewPager.setOffscreenPageLimit(1);
			viewPager.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (gestureDetector != null) {
						return gestureDetector.onTouchEvent(event);
					} else {
						return false;
					}
				}
			});
			addView(viewPager);
	}

	private void initFlipBookGallery() {
		ViewStub viewStub = (ViewStub) findViewById(R.id.image_pager_image_stub);
		final View view = viewStub.inflate();
        flipBookText = (TextView) view.findViewById(R.id.slideshow_item_text);
		flipBookImageView = (ImageView) view.findViewById(R.id.slideshow_item_image);
		flipBookImageView.setBackgroundColor(backgroundColor);
		flipBookImageView.setOnTouchListener(new OnTouchListener() {
            private float lastImageX, dx;

            ViewConfiguration vc = ViewConfiguration.get(getContext());
            final int touchSlop = vc.getScaledTouchSlop() * 3;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        lastImageX = event.getX();
                        getBitmapAsyncTaskCancelled = false;
                        break;
                    case (MotionEvent.ACTION_MOVE):
                        getBitmapAsyncTaskCancelled = false;
                        dx = event.getX() - lastImageX;
                        if (dx > touchSlop) {
                            setCurrentPosition(currentImageViewPosition + 1, false);
                            lastImageX += touchSlop;
                        } else if (dx < -touchSlop) {
                            setCurrentPosition(currentImageViewPosition - 1, false);
                            lastImageX -= touchSlop;
                        }
                        break;
                    case (MotionEvent.ACTION_UP):
                        getBitmapAsyncTaskCancelled = true;
                        break;
                }
                if (gestureDetector != null) {
                    return gestureDetector.onTouchEvent(event);
                }
                return true;

            }
        });
	}

	private void setCurrentImageViewPosition(int position) {
		currentImageViewPosition = position;
		int actualPosition = slidesInfo.count - position;
		String path = slidesInfo.getFullPathToImage(actualPosition);
		new GetBitmapAsyncTask(false) {
			@Override
			protected void onPostExecute(Bitmap bmp) {
				if (isCancelled()) {
					return;
				}
				if (flipBookImageView != null && bmp != null) {
					flipBookImageView.setImageBitmap(bmp);
				} else {

                	Log.w("ImageLayout", "setCurrentImageViewPosition Bitmap null");
                    flipBookImageView.setVisibility(View.GONE);
                    flipBookText.setVisibility(View.VISIBLE);
                }
				super.onPostExecute(bmp);
			}
		}.execute(path);
	}

	private void initSingleImage() {
		ViewStub viewStub = (ViewStub) findViewById(R.id.image_pager_image_stub);
		final View view = viewStub.inflate();
		final ImageView imageView = (ImageView) view
				.findViewById(R.id.slideshow_item_image);
        final TextView text = (TextView) view.findViewById(R.id.slideshow_item_text);
		new GetBitmapAsyncTask(true) {
			@Override
			protected void onPostExecute(Bitmap bmp) {
				if (isCancelled()) {
					return;
				}
				view.setBackgroundColor(backgroundColor);
                if (bmp == null) {
                	Log.w("ImageLayout", "Single Image Bitmap null");
                    imageView.setVisibility(View.GONE);
                    text.setVisibility(View.VISIBLE);
                } else {
				    imageView.setImageBitmap(bmp);
                }
				super.onPostExecute(bmp);
			}
		}.execute(slidesInfo.getFullPathToImage(1));
	}

	@Override
	public void setBackgroundColor(int color) {
		backgroundColor = color;
		super.setBackgroundColor(color);
	}

	private class SlidesInfo {
		public final String assetDir;
		public int count;
		public String preffix;
		public String suffix;

		public SlidesInfo(String basePath) {
				File file = new File(basePath);
				this.assetDir = file.getParent();
				String fileName = file.getName();
			try {
				this.count = Integer
						.valueOf(fileName.split("_")[1].split("\\.")[0]);
				this.preffix = fileName.split("_")[0];
				this.suffix = fileName.split("_")[1].split("\\.")[1];
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		public String getFullPathToImage(int position) {
			return this.assetDir + "/" + this.preffix + "_"
					+ String.valueOf(position) + "." + this.suffix;
		}

		@Override
		public String toString() {
			return "SlidesInfo [" + "assetDir=" + assetDir + ", count=" + count
					+ ", preffix=" + preffix + ", suffix=" + suffix + "]";
		}

	}

	private class GetBitmapAsyncTask extends AsyncTask<String, Void, Bitmap> {
		
		private boolean showProgressBar = false;

		public GetBitmapAsyncTask(boolean showProgressBar) {
			this.showProgressBar = showProgressBar;
			
		}
		@Override
		protected void onPreExecute() {
			if (showProgressBar) {
				progressBar.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(String... paths) {
			if (!getBitmapAsyncTaskCancelled) {
				return SystemHelper.decodeSampledBitmapFromFile(paths[0],
                        getWidth(), getHeight());
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	protected class SimpleImageAdapter extends PagerAdapter {

		protected Context context;

		private final SlidesInfo slidesInfo;

		public SimpleImageAdapter(Context context, SlidesInfo slidesInfo) {
			this.context = context;
			this.slidesInfo = slidesInfo;
		}

		@Override
		public int getCount() {
			return slidesInfo.count;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		public String getItem(int position) {
			// for reversive image showing
			position = slidesInfo.count - position;
			return slidesInfo.getFullPathToImage(position);
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			String path = getItem(position % slidesInfo.count);

			final View view = inflater.inflate(R.layout.slideshow_item_layout,
					null);
			view.setTag(position);

			final ImageView img = (ImageView) view
					.findViewById(R.id.slideshow_item_image);
            final TextView text = (TextView) view.findViewById(R.id.slideshow_item_text);
			final FrameLayout background = (FrameLayout) view
					.findViewById(R.id.slide_show_frame);
            background.setBackgroundColor(backgroundColor);
			new GetBitmapAsyncTask(true) {
				@Override
				protected void onPostExecute(Bitmap bmp) {
					if (isCancelled()) {
						return;
					}
                    if (bmp == null) {
                    	Log.w("ImageLayout", "ViewPager Image Bitmap null");
                        img.setVisibility(View.GONE);
                        text.setVisibility(View.VISIBLE);
                    } else {
					    img.setImageBitmap(bmp);
                    }
					super.onPostExecute(bmp);
				}
			}.execute(path);
			container.addView(view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		public int getSlideCount() {
			return slidesInfo.count;
		}
	}
}
