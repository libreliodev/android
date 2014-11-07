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
    private boolean error;

    private EditText username;
    private EditText password;

	private OnUsernamePasswordLoginListener onUsernamePasswordLoginListener;

	public interface OnUsernamePasswordLoginListener {
		public void onEnterUsernamePasswordLogin(String username, String password);
        public void onCancel();
	}

	public UsernamePasswordLoginDialog(Context context, String title, boolean error) {
		this.context = context;
		this.title = title;
        this.error = error;
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

        if (error) {
            view.findViewById(R.id.error_text).setVisibility(View.VISIBLE);
        }
		
		builder.setView(view);
	
		// Set up the buttons
		builder.setPositiveButton(context.getString(R.string.login), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		    	if (null != onUsernamePasswordLoginListener){
		    		onUsernamePasswordLoginListener.onEnterUsernamePasswordLogin(
                            username.getText().toString().trim(), password.getText().toString().trim());
		    	}
		    }
		});
		
		builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        onUsernamePasswordLoginListener.onCancel();
		    }
		});
	} 
	
	public void setOnUsernamePasswordLoginListener(OnUsernamePasswordLoginListener onUsernamePasswordLoginListener){
		this.onUsernamePasswordLoginListener = onUsernamePasswordLoginListener;
	}
	
	public void show(){
		if (null != builder){
			builder.show();
		}
	}
}
