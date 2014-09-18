package com.librelio.library.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public abstract class BaseNivealesActivity extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
	}
	
	public void init(){
	}
}
