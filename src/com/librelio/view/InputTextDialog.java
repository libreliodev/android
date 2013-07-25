package com.librelio.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

public class InputTextDialog {

	private static String OK = "OK";
	private static String Cancel = "Cancel";
	
	private AlertDialog.Builder builder;
	private Context context;
	private String title;
	
	private OnEnterValueListener onEnterValueListener;
	
	public interface OnEnterValueListener {
		public void onEnterValue(String value);
	}
	
	public InputTextDialog(Context context, String title) {
		this.context = context;
		this.title = title;
		configureDialog();
	}
	
	private void configureDialog(){

		builder = new AlertDialog.Builder(context);
			
		if (null != title){
			builder.setTitle(title);
		}
		
		final EditText input = constructInputView(false);
		
		builder.setView(input);
	
		// Set up the buttons
		builder.setPositiveButton(OK, new DialogInterface.OnClickListener() { 
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	if (null != onEnterValueListener){
		    		onEnterValueListener.onEnterValue(
		    				input.getText().toString());
		    	}
		    }
		});
		
		builder.setNegativeButton(Cancel, new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.cancel();
		    }
		});
	} 
	
	private EditText constructInputView(boolean asPassword){
		
		// Set up the input
		EditText input = new EditText(context);
		
		// Specify the type of input expected;
		int inputType = InputType.TYPE_CLASS_TEXT;
		//sets the input as a password, and will mask the text
		if (asPassword){
			inputType = inputType | InputType.TYPE_TEXT_VARIATION_PASSWORD;
		}
		input.setInputType(inputType);
		
		return input;
	}
	
	public void setOnEnterValueListener(OnEnterValueListener onEnterValueListener){
		this.onEnterValueListener = onEnterValueListener;
	}
	
	public void show(){
		if (null != builder){
			builder.show();
		}
	}
}
