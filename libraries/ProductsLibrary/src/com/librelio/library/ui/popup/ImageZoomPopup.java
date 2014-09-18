package com.librelio.library.ui.popup;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class ImageZoomPopup extends CustomPopupWindow {

	private final Context context;

	protected static final int ANIM_GROW_FROM_LEFT = 1;
	protected static final int ANIM_GROW_FROM_RIGHT = 2;
	protected static final int ANIM_GROW_FROM_CENTER = 3;
	protected static final int ANIM_REFLECT = 4;
	protected static final int ANIM_AUTO = 5;

	protected static final int MAX_IMAGE_ZOOM = 3;

	private ArrayList<ActionItem> actionList;
	private LayoutParams mLayoutParams;
	private ImageView mImageView;
	private Bitmap mImageViewBitmap;

	private int[] myCoords;

	private int mWidth;

	private int mHeight;

	private float mImageScale = 1;

	public ImageZoomPopup(View anchor, int[] pCoords) {
		super(anchor);
		
		myCoords = pCoords;
		context = anchor.getContext();

//		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		root = inflater.inflate(R.layout.product_detail_popup, null);


		mImageView = new ImageView(context);
//		mImageView = (ImageView) root.findViewById(R.id.ZoomedProductImage);
		
		mImageView.setScaleType(ScaleType.CENTER);
		mImageView.setAdjustViewBounds(false);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		mImageView.setLayoutParams(lp);

		root = mImageView;
		setContentView(getRootView());
	}


	public void setImageBitmap(Bitmap b) {
		if( mWidth > 0 && mHeight > 0)
		{

			mImageViewBitmap = Bitmap.createScaledBitmap(b, mImageViewBitmap.getWidth(), mImageViewBitmap.getHeight(), true);
			b.recycle();

		} else {
			mImageViewBitmap = b;
		}
		recycleImageViewBitmap(mImageView);
		mImageView.setImageBitmap(mImageViewBitmap);
	}

	public void addActionItem(ActionItem action) {
		actionList.add(action);
	}

	public void show() {
		onShow();
		float density = context.getResources().getDisplayMetrics().density;
		mWidth = (anchor.getWidth() == 0 ) ? Math.round(density*myCoords[5]) : anchor.getWidth();
		mHeight = Math.round(density*myCoords[3]);
		int xPos = (anchor.getWidth() == 0 ) ? Math.round(density*myCoords[4]) : 0;
		int yPos = Math.round(density*myCoords[2]);
		
		window.setWidth(mWidth);
		window.setTouchable(true);
		window.setFocusable(true);
		window.setOutsideTouchable(true);
//		window.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.popover_background));
//		mImageView.setPadding(10, 10, 10, 10);
		window.setContentView(root);

		
		
		int[] location = new int[2];

		anchor.getLocationOnScreen(location);

		xPos += location[0];
		yPos += location[1];
		mHeight = (int) Math.round(yPos*0.75); 

//		yPos = windowYPos;
		
		if (mLayoutParams == null)
			mLayoutParams = new LayoutParams(mWidth,
					mHeight);
		getRootView().setLayoutParams(mLayoutParams);
		getRootView().measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		getRootView().measure(
				MeasureSpec.EXACTLY | mWidth,
				MeasureSpec.EXACTLY | mHeight);

		mImageScale  = (mWidth * MAX_IMAGE_ZOOM > 2048) ? 2048 / mWidth : (float) MAX_IMAGE_ZOOM;
		if(mImageViewBitmap != null) {
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(mImageViewBitmap, Math.round(mImageScale * mImageViewBitmap.getWidth()), Math.round(mImageScale * mImageViewBitmap.getHeight()), true);
			recycleImageViewBitmap(mImageView);
			mImageView.setImageBitmap(resizedBitmap);
			mImageViewBitmap = resizedBitmap;
			int bitmapHeight = mImageViewBitmap.getHeight();
			if(bitmapHeight < mHeight) {
				mHeight = bitmapHeight;
			}
		}
		window.setHeight(mHeight);

		
		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos - mHeight);
		scroll(myCoords);
	}

	

	


	@Override
	public void dismiss() {
		super.dismiss();
		this.recycleImageViewBitmap(mImageView);
	}

	public View getRootView() {
		return root;
	}

	public void setLayoutParams(LayoutParams params) {
		mLayoutParams = params;
	}


	
	public void recycleImageViewBitmap(ImageView i) {
		if(i != null) {
			Drawable d = i.getDrawable();
			if (d instanceof BitmapDrawable) {
				BitmapDrawable bd = (BitmapDrawable) d;
				bd.getBitmap().recycle();
			}
		}
	}

	
	public void scroll(int[] coords) {
		float absXTouch = (float)mImageViewBitmap.getWidth() - ((float)coords[5] - (float)coords[0] + (float) coords[4])/(float)coords[5] * mImageViewBitmap.getWidth() - mImageViewBitmap.getWidth()/2;
		absXTouch = (absXTouch < -mImageViewBitmap.getWidth() /2 + mWidth / 2) ? -mImageViewBitmap.getWidth() /2 + mWidth / 2 : absXTouch;
		absXTouch = (absXTouch > mImageViewBitmap.getWidth() /2 - mWidth / 2) ? mImageViewBitmap.getWidth() /2 - mWidth / 2 : absXTouch;

		
		float absYTouch = (float)mImageViewBitmap.getHeight() - ((float)coords[3] - (float)coords[1] + (float)coords[2])/(float)coords[3] * mImageViewBitmap.getHeight() - mImageViewBitmap.getHeight()/2;
		absYTouch = (absYTouch < -mImageViewBitmap.getHeight() /2 + mHeight / 2) ? -mImageViewBitmap.getHeight() /2 + mHeight / 2 : absYTouch;
		absYTouch = (absYTouch > mImageViewBitmap.getHeight() /2 - mHeight / 2) ? mImageViewBitmap.getHeight() /2 - mHeight / 2 : absYTouch;

		
		
		mImageView.scrollTo(Math.round(absXTouch), Math.round(absYTouch));
	}
	/**
	 * @param pXPos
	 * @param pI
	 */
	public void scrollTo(int pXPos, int pYPos) {
		
		mImageView.scrollTo(pXPos, pYPos);
	}

}
