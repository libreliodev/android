package com.librelio.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artifex.mupdfdemo.AsyncTask;
import com.artifex.mupdfdemo.MuPDFCore;
import com.niveales.wind.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

/**
 * @author Dmitry Valetin
 * 
 */
public class PDFPreviewPagerAdapter extends RecyclerView.Adapter<PDFPreviewPagerAdapter.PDFPreviewViewHolder> {

    public static class PDFPreviewViewHolder extends RecyclerView.ViewHolder {

        ImageView previewPageImageView = null;
        TextView previewPageNumber = null;
        LinearLayout previewPageLinearLayout = null;

        public PDFPreviewViewHolder(View view) {
            super(view);
            this.previewPageImageView = (ImageView) view
                    .findViewById(R.id.PreviewPageImageView);
            this.previewPageNumber = (TextView) view
                    .findViewById(R.id.PreviewPageNumber);
            this.previewPageLinearLayout = (LinearLayout) view
                    .findViewById(R.id.PreviewPageLinearLayout);
        }
    }

	private static final String TAG = PDFPreviewPagerAdapter.class
			.getSimpleName();
	private Context mContext;
	private MuPDFCore mCore;

	private Point mPreviewSize;
	private final SparseArray<Bitmap> mBitmapCache = new SparseArray<Bitmap>();
	private String mPath;
	
	private int currentlyViewing;
	private Bitmap mLoadingBitmap;

	public PDFPreviewPagerAdapter(Context context, MuPDFCore core) {
		mContext = context;
		mCore = core;
		mPath = core.getFileDirectory() + "/previewcache/";
		File mCacheDirectory = new File(mPath);
		if (!mCacheDirectory.exists())
			mCacheDirectory.mkdirs();

		mLoadingBitmap = BitmapFactory.decodeResource(
				mContext.getResources(), R.drawable.darkdenim3);

		mPreviewSize = new Point();
		mPreviewSize.x = mContext.getResources()
				.getDimensionPixelSize(R.dimen.page_preview_size_width);
		mPreviewSize.y = mContext.getResources()
				.getDimensionPixelSize(R.dimen.page_preview_size_height);
	}

    @Override
    public PDFPreviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.preview_pager_item_layout, parent, false);
        return new PDFPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PDFPreviewViewHolder holder, int position) {
        holder.previewPageNumber.setText(String.valueOf(position + 1));
        holder.previewPageLinearLayout.setBackgroundColor(Color.TRANSPARENT);
        drawPageImageView(holder, position);
    }

    @Override
	public long getItemId(int pPosition) {
		if(mCore.getDisplayPages() == 1) 
			return pPosition;
		else
			if(pPosition > 0)
				return (pPosition + 1) / 2;
			else 
				return 0;
	}

    @Override
    public int getItemCount() {
        return mCore.countSinglePages();
    }

	private void drawPageImageView(PDFPreviewViewHolder holder, int position) {
		if (cancelPotentialWork(holder, position)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(holder, position);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(
					mContext.getResources(), mLoadingBitmap, task);
			holder.previewPageImageView.setImageDrawable(asyncDrawable);
			task.execute();
		}
	}

	public static boolean cancelPotentialWork(PDFPreviewViewHolder holder, int position) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(holder.previewPageImageView);

		if (bitmapWorkerTask != null) {
			final int bitmapPosition = bitmapWorkerTask.position;
			if (bitmapPosition != position) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was
		// cancelled
		return true;
	}

	class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {

		private final WeakReference<PDFPreviewViewHolder> viewHolderReference;
		private int position;

		public BitmapWorkerTask(PDFPreviewViewHolder holder, int position) {
			viewHolderReference = new WeakReference<PDFPreviewViewHolder>(holder);
			this.position = position;
		}

		@Override
		protected Bitmap doInBackground(Integer... params) {
			Bitmap lq = getCachedBitmap(position);
			mBitmapCache.put(position, lq);
			return lq;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

			if (viewHolderReference != null && bitmap != null) {
				final PDFPreviewViewHolder holder = viewHolderReference.get();
				if (holder != null) {
					final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(holder.previewPageImageView);
					if (this == bitmapWorkerTask && holder != null) {
						holder.previewPageImageView.setImageBitmap(bitmap);
						holder.previewPageNumber.setText(String
								.valueOf(position + 1));
						if (getCurrentlyViewing() == position
								|| (mCore.getDisplayPages() == 2 && getCurrentlyViewing() == position - 1)) {
							holder.previewPageLinearLayout
									.setBackgroundColor(mContext
											.getResources()
											.getColor(
													R.color.thumbnail_selected_background));
						} else {
							holder.previewPageLinearLayout
									.setBackgroundColor(Color.TRANSPARENT);
						}
					}
				}
			}
		}
	}

	private Bitmap getCachedBitmap(int position) {
		String mCachedBitmapFilePath = mPath + position + ".jpg";
		File mCachedBitmapFile = new File(mCachedBitmapFilePath);
		Bitmap lq = null;
		try {
			if (mCachedBitmapFile.exists() && mCachedBitmapFile.canRead()) {
				Log.d(TAG, "page " + position + " found in cache");
				lq = BitmapFactory.decodeFile(mCachedBitmapFilePath);
				return lq;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// some error with cached file,
			// delete the file and get rid of bitmap
			mCachedBitmapFile.delete();
			lq = null;
		}
		if (lq == null) {
			lq = Bitmap.createBitmap(mPreviewSize.x, mPreviewSize.y,
					Bitmap.Config.ARGB_8888);
			mCore.drawSinglePage(position, lq, mPreviewSize.x, mPreviewSize.y);
			try {
				lq.compress(CompressFormat.JPEG, 50, new FileOutputStream(
						mCachedBitmapFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mCachedBitmapFile.delete();
			}
		}
		return lq;
	}

	public int getCurrentlyViewing() {
		return currentlyViewing;
	}

	public void setCurrentlyViewing(int currentlyViewing) {
        this.currentlyViewing = currentlyViewing;
        notifyDataSetChanged();
    }

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}
}
