package com.librelio.view;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.niveales.wind.R;

public class UsernamePasswordLoginDialog {

	private Context context;
	private String title;
    private boolean error;

	private OnUsernamePasswordLoginListener onUsernamePasswordLoginListener;
	private MaterialDialog dialog;

	public interface OnUsernamePasswordLoginListener {
		void onEnterUsernamePasswordLogin(String username, String password);
        void onCancel();
	}

	public UsernamePasswordLoginDialog(Context context, String title, boolean error) {
		this.context = context;
		this.title = title;
        this.error = error;
        configureDialog();
	}
	
	private void configureDialog(){
		dialog = new MaterialDialog.Builder(context)
				.title(title)
				.customView(R.layout.username_password_login_dialog, false)
				.cancelable(false)
				.autoDismiss(false)
				.positiveText(R.string.login)
				.negativeText(R.string.cancel)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						super.onPositive(dialog);
						EditText username = (EditText) dialog.findViewById(R.id.username);
						if (TextUtils.isEmpty(username.getText().toString())) {
							username.setError(context.getString(R.string.username_cannot_be_empty));
							return;
						}
						EditText password = (EditText) dialog.findViewById(R.id.password);
						if (TextUtils.isEmpty(password.getText().toString())) {
							password.setError(context.getString(R.string.password_cannot_be_empty));
							return;
						}
						onUsernamePasswordLoginListener.onEnterUsernamePasswordLogin(
								username.getText().toString().trim(),
								password.getText().toString().trim());
						dialog.dismiss();
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						super.onNegative(dialog);
						onUsernamePasswordLoginListener.onCancel();
						dialog.dismiss();
					}
				})
				.build();

		if (error) {
			EditText password = (EditText) dialog.findViewById(R.id.password);
			password.setError(context.getString(R.string.incorrect_username_or_password));
		}
	}
	
	public void setOnUsernamePasswordLoginListener(OnUsernamePasswordLoginListener onUsernamePasswordLoginListener){
		this.onUsernamePasswordLoginListener = onUsernamePasswordLoginListener;
	}
	
	public void show(){
		if (null != dialog){
			dialog.show();
		}
	}
}
