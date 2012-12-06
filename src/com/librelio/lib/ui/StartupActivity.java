package com.librelio.lib.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.niveales.wind.R;

public class StartupActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.startup);
		new Timer().schedule(new TimerTask() {          
		    @Override
		    public void run() {
		        Intent intent = new Intent(getApplicationContext(), MainMagazineActivity.class);
		        startActivity(intent);
		        finish();
		    }
		}, 2000);
	}
	
}
