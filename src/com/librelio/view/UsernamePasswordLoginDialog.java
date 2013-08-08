package com.librelio.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import com.niveales.wind.R;

public class UsernamePasswordLoginDialog {

	private AlertDialog.Builder builder;
	private Context context;
	private String title;

    private EditText username;
    private EditText password;

	private OnEnterUsernamePasswordLoginListener onEnterUsernamePasswordLoginListener;

	public interface OnEnterUsernamePasswordLoginListener {
		public void onEnterUsernamePasswordLogin(String username, String password);
	}

	public UsernamePasswordLoginDialog(Context context, String title) {
		this.context = context;
		this.title = title;
		configureDialog();
	}
	
	private void configureDialog(){

		builder = new AlertDialog.Builder(context);
			
		if (null != title){
			builder.setTitle(title);
		}

        View view = LayoutInflater.from(context).inflate(R.layout.username_password_login_dialog, null, false);
        username = (EditText) view.findViewById(R.id.username);
        password = (EditText) view.findViewById(R.id.password);
		
		builder.setView(view);
	
		// Set up the buttons
		builder.setPositiveButton(context.getString(R.string.login), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	if (null != onEnterUsernamePasswordLoginListener){
		    		onEnterUsernamePasswordLoginListener.onEnterUsernamePasswordLogin(
                            username.getText().toString(), password.getText().toString());
		    	}
		    }
		});
		
		builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        dialog.cancel();
		    }
		});
	} 
	
	public void setOnEnterUsernamePasswordLoginListener(OnEnterUsernamePasswordLoginListener onEnterUsernamePasswordLoginListener){
		this.onEnterUsernamePasswordLoginListener = onEnterUsernamePasswordLoginListener;
	}
	
	public void show(){
		if (null != builder){
			builder.show();
		}
	}
}
