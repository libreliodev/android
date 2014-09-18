/**
 * 
 */
package com.librelio.library.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * @author Dmitry Valetin
 *
 */
public class BaseNivealesFragment extends Fragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		this.setRetainInstance(true);
	}
	
	public boolean onBackPressed() {
		return false;
	}

}
