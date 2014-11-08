package com.librelio.products.ui.popup;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.niveales.wind.R;

import java.util.ArrayList;

public class QuickAction extends CustomPopupWindow {
//	private final View root;
	private final ImageView mArrowUp;
	private final ImageView mArrowDown;
	private final LayoutInflater inflater;
	private final Context context;
	
	protected static final int ANIM_GROW_FROM_LEFT = 1;
	protected static final int ANIM_GROW_FROM_RIGHT = 2;
	protected static final int ANIM_GROW_FROM_CENTER = 3;
	protected static final int ANIM_REFLECT = 4;
	protected static final int ANIM_AUTO = 5;
	
	private int animStyle;
	private ViewGroup mTrack;
	private ScrollView scroller;
	private ArrayList<ActionItem> actionList;
	private LayoutParams mLayoutParams;
	
	public QuickAction(View anchor) {
		super(anchor);
		
		actionList	= new ArrayList<ActionItem>();
		context		= anchor.getContext();
		inflater 	= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		root		= (ViewGroup) inflater.inflate(R.layout.popup_v, null);
		
		mArrowDown 	= (ImageView) getRootView().findViewById(R.id.arrow_down);
		mArrowUp 	= (ImageView) getRootView().findViewById(R.id.arrow_up);
		
		setContentView(getRootView());
	    
		mTrack 			= (ViewGroup) getRootView().findViewById(R.id.tracks);
		scroller		= (ScrollView) getRootView().findViewById(R.id.scroller);
		animStyle		= ANIM_AUTO;
	}

	public void setAnimStyle(int animStyle) {
		this.animStyle = animStyle;
	}

	public void addActionItem(ActionItem action) {
		actionList.add(action); 
	}
	
	public void show () {
		preShow();
		
		int xPos, yPos;
		
		int[] location 		= new int[2];
	
		anchor.getLocationOnScreen(location);

		Rect anchorRect 	= new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
		                	+ anchor.getHeight());

		createActionList();
		
		if(mLayoutParams == null)
			mLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		getRootView().setLayoutParams(mLayoutParams);
		getRootView().measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		getRootView().measure(MeasureSpec.AT_MOST | getRootView().getMeasuredWidth(), MeasureSpec.AT_MOST | getRootView().getMeasuredHeight());
	
		int rootHeight 		= getRootView().getMeasuredHeight();
		int rootWidth		= getRootView().getMeasuredWidth();
		
		int screenWidth 	= windowManager.getDefaultDisplay().getWidth();
		int screenHeight	= windowManager.getDefaultDisplay().getHeight();
		
	if ((anchorRect.left + rootWidth) > screenWidth) {
			xPos = anchorRect.left - (rootWidth-anchor.getWidth());
		} else {
			if (anchor.getWidth() > rootWidth) {
				xPos = anchorRect.centerX() - (rootWidth/2);
			} else {
				xPos = anchorRect.left;
			}
		}
		
		int dyTop			= anchorRect.top;
		int dyBottom		= screenHeight - anchorRect.bottom;

		boolean onTop		= (dyTop > dyBottom) ? true : false;

		if (onTop) {
			if (rootHeight > dyTop) {
				yPos 			= 15;
				LayoutParams l 	= scroller.getLayoutParams();
				l.height		= dyTop - anchor.getHeight();
			} else {
				yPos = anchorRect.top - rootHeight;
			}
		} else {
			yPos = anchorRect.bottom;
			
			if (rootHeight > dyBottom) { 
				LayoutParams l 	= scroller.getLayoutParams();
				l.height		= dyBottom;
				getRootView().setLayoutParams(l);
			}
		}
		
		showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), anchorRect.centerX()-xPos);
		
		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
		
		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
	private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {
		int arrowPos = requestedX - mArrowUp.getMeasuredWidth()/2;

		switch (animStyle) {
		case ANIM_GROW_FROM_LEFT:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			break;
					
		case ANIM_GROW_FROM_RIGHT:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			break;
					
		case ANIM_GROW_FROM_CENTER:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
		break;
			
		case ANIM_REFLECT:
			window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
		break;
		
		case ANIM_AUTO:
			if (arrowPos <= screenWidth/4) {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			} else if (arrowPos > screenWidth/4 && arrowPos < 3 * (screenWidth/4)) {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
			} else {
				window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			}
					
			break;
		}
	}
	
	private void createActionList() {
		View view;
		String title;
		Drawable icon;
		
		OnClickListener listener;
	
		for (int i = 0; i < actionList.size(); i++) {
			title 		= actionList.get(i).getTitle();
			icon 		= actionList.get(i).getIcon();
			listener	= actionList.get(i).getListener();
			view		= actionList.get(i).getActionItemView();
			if(view == null) {
				view 		= getActionItem(title, icon, listener);
			}
			
			view.setFocusable(true);
			view.setClickable(true);
			 
			mTrack.addView(view);
		}
	}
	
	private View getActionItem(String title, Drawable icon, OnClickListener listener) {
		LinearLayout container	= (LinearLayout) inflater.inflate(R.layout.action_item, null);
		
		ImageView img			= (ImageView) container.findViewById(R.id.icon);
		TextView text			= (TextView) container.findViewById(R.id.title);
		
		if (icon != null) {
			img.setImageDrawable(icon);
		}
		
		if (title != null) {			
			text.setText(title);
		}
		
		if (listener != null) {
			container.setOnClickListener(listener);
		}

		return container;
	}
	

	private void showArrow(int whichArrow, int requestedX) {
        final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
        final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

        final int arrowWidth = mArrowUp.getMeasuredWidth();

        showArrow.setVisibility(View.VISIBLE);
        
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)showArrow.getLayoutParams();
       
        param.leftMargin = requestedX - arrowWidth / 2;
        
        hideArrow.setVisibility(View.INVISIBLE);
    }
	
	@Override
	public void dismiss() {
		super.dismiss();
		mTrack.removeAllViews();
	}

	public View getRootView() {
		return root;
	}
	
	public void setLayoutParams(LayoutParams params) {
		mLayoutParams = params;
	}

	/**
	 * @return
	 */
	public int getWidth() {
		int w = root.getWidth();
		int mw = root.getMeasuredWidth();
		return mw;
	}
	
}
