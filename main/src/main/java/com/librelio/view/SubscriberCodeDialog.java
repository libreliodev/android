package com.librelio.view;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.niveales.wind.R;

public class SubscriberCodeDialog {

    private final boolean error;
	private Context context;
	private String title;
	
	private OnSubscriberCodeListener onSubscriberCodeListener;
    private EditText subscriberCode;
	private MaterialDialog dialog;

	public interface OnSubscriberCodeListener {
		void onEnterValue(String value);
        void onCancel();
	}
	
	public SubscriberCodeDialog(Context context, String title, boolean error) {
		this.context = context;
		this.title = title;
        this.error = error;
		configureDialog();
	}
	
	private void configureDialog(){
		dialog = new MaterialDialog.Builder(context)
				.title(title)
				.customView(R.layout.subscriber_code_dialog, false)
				.cancelable(false)
				.autoDismiss(false)
				.positiveText(R.string.login)
				.negativeText(R.string.cancel)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						super.onPositive(dialog);
						EditText code = (EditText) dialog.findViewById(R.id.subscriber_code);
						if (TextUtils.isEmpty(code.getText().toString())) {
							code.setError(context.getString(R.string
									.subscriber_code_cannot_be_empty));
							return;
						}
						onSubscriberCodeListener.onEnterValue(code.getText().toString().trim());
						dialog.dismiss();
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						super.onNegative(dialog);
						onSubscriberCodeListener.onCancel();
						dialog.dismiss();
					}
				})
				.build();

		if (error) {
			EditText subscriberCode = (EditText) dialog.findViewById(R.id.subscriber_code);
			subscriberCode.setError(context.getString(R.string.incorrect_code));
		}
	}
	
	public void setSubscriberCodeListener(OnSubscriberCodeListener onSubscriberCodeListener){
		this.onSubscriberCodeListener = onSubscriberCodeListener;
	}

	public void show(){
		if (null != dialog){
			dialog.show();
		}
	}
}
