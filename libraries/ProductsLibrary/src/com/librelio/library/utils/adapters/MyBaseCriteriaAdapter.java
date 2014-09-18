package com.librelio.library.utils.adapters;

import android.widget.BaseAdapter;

public abstract class MyBaseCriteriaAdapter extends BaseAdapter {
	String headerText;
	AdapterCriteriaChangeListener listener;

	public String getHeaderText() {
		return headerText;
	}

	public void setHeaderText(String text) {
		headerText = text;
	}
	
	public AdapterCriteriaChangeListener getCriteriaChangedListener() {
		return listener;
	}
	
	public void setOnCriteriaCangeListener(AdapterCriteriaChangeListener l) {
		listener = l;
	}
	
	@Override 
	public void notifyDataSetChanged() {
		if(listener != null)
			listener.onAdapterCriteriaChanged();
	}
	
	public interface AdapterCriteriaChangeListener {
		public void onAdapterCriteriaChanged();
	}
}
