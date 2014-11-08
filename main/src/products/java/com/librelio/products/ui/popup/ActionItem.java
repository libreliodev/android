package com.librelio.products.ui.popup;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;

public class ActionItem {
	private Drawable icon;
	private String title;
	private OnClickListener listener;
	private String mTag;
	private View mActionItemView;
	
	public ActionItem() {}
	
	public ActionItem(Drawable icon) {
		this.icon = icon;
	}
	
	/**
	 * @param pSearchView
	 */
	public ActionItem(View pView) {
		mActionItemView = pView;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public Drawable getIcon() {
		return this.icon;
	}
	

	public void setOnClickListener(OnClickListener listener) {
		this.listener = listener;
	}
	
	public OnClickListener getListener() {
		return this.listener;
	}
	
	public void setTag(String tag) {
		mTag = tag;
	}
	
	public String getTag() {
		return mTag;
	}
	
	public void setActionItemView(View v) {
		mActionItemView = v;
	}
	
	public View getActionItemView() {
		return mActionItemView;
	}
}
