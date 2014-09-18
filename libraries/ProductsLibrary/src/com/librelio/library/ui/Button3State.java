package com.librelio.library.ui;


import com.niveales.testskis.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * 
 * @author valetin Button3State - is a 3 state button, which changes
 *         android:drawableRight each time button is pressed
 */
public class Button3State extends Button implements OnClickListener {

	public int state = 0;
	public boolean stateChecked = false;
	public Drawable state1drawable;
	public Drawable state2drawable;
	public Drawable state3drawable;
	public Drawable state1background;
	public Drawable state2background;
	public Drawable state3background;
	
	private OnStateChanged listener;

	/**
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 *            - should include "custom:state1drawable",
	 *            "custom:state2drawable" and "custom:state3drawable"
	 */

	public Button3State(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public Button3State(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, R.styleable.Button3State);
	}

	private void init(Context context, AttributeSet attrs, int[] styleable) {
		TypedArray a = context.obtainStyledAttributes(attrs, styleable);
		int id = a.getResourceId(R.styleable.Button3State_state1drawable, R.drawable.empty);
		this.state1drawable = context.getResources().getDrawable(id);
		id = a.getResourceId(R.styleable.Button3State_state2drawable, R.drawable.mark_up);
		this.state2drawable = context.getResources().getDrawable(id);
		id = a.getResourceId(R.styleable.Button3State_state3drawable, R.drawable.mark_down);
		this.state3drawable = context.getResources().getDrawable(id);
		
		id = a.getResourceId(R.styleable.Button3State_state1background, R.drawable.iphone_gamme_unselected);
		this.state1background = context.getResources().getDrawable(id);
		id = a.getResourceId(R.styleable.Button3State_state2background, R.drawable.iphone_gamme_unselected);
		this.state2background = context.getResources().getDrawable(id);
		id = a.getResourceId(R.styleable.Button3State_state3background, R.drawable.iphone_gamme_unselected);
		this.state3background = context.getResources().getDrawable(id);
		
		
		
		a.recycle();
		super.setOnClickListener(this);
	}

	public void setStateDrawables(Drawable state1drawable,
			Drawable state2drawable, Drawable state3drawable) {
		this.state1drawable = state1drawable;
		this.state2drawable = state2drawable;
		this.state3drawable = state3drawable;

	}

	// @Override
	// public void setOnClickListener(OnClickListener l) {
	//
	// }

	/**
	 * 
	 * @param listener
	 *            - an implementation of OnStateChanged interface
	 */
	public void setOnStateChangeListener(OnStateChanged listener) {
		this.listener = listener;
	}

	@Override
	public void onClick(View v) {
		state++;
		if (state > 2)
			state = 0;
		updateStateDrawable();
		if (listener != null)
			listener.onStateChanged(Button3State.this, state);
	}

	private void updateStateDrawable() {
		switch (state) {
		case 0: {
			this.setCompoundDrawablesWithIntrinsicBounds(null, null, state1drawable, null);
			this.setBackgroundDrawable(state1background);
			break;
		}
		case 1: {
			this.setCompoundDrawablesWithIntrinsicBounds(null, null, state2drawable, null);
			this.setBackgroundDrawable(state2background);
			break;
		}
		case 2: {
			this.setCompoundDrawablesWithIntrinsicBounds(null, null, state3drawable, null);
			this.setBackgroundDrawable(state3background);
			break;
		}
		}
		this.invalidate();
	}

	public void setState(int state) {
		this.state = state;
		this.updateStateDrawable();
	}
	/**
	 * 
	 * @author valetin Interface for state change callbacks
	 */
	public interface OnStateChanged {
		public void onStateChanged(Button3State pView, int state);
	}

}
