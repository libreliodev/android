package com.librelio.lib.ui;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.librelio.base.BaseActivity;
import com.librelio.lib.LibrelioApplication;
import com.niveales.wind.R;

public class BillingActivity extends BaseActivity {
	private static final String TAG = "BillingActivity";
	private static final int CALLBACK_CODE = 101;
	
	private static final String serverURL = "http://php.netcook.org/librelio-server/downloads/android_verify.php";
	private String fileName;
	private String title;
	private String subtitle;
	private String productId;
	private String productPrice;
	private IInAppBillingService mService;
	private Button buy;
	private Button cancel;
	private Button subs;

	
	private Context getContext(){
		return this;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wait_bar);
		if(!isNetworkConnected()){
			showDialog(CONNECTION_ALERT);
		} else {
			bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
						mServiceConn, Context.BIND_AUTO_CREATE);
			fileName = getIntent().getExtras().getString(DownloadActivity.FILE_NAME_KEY);
			title = getIntent().getExtras().getString(DownloadActivity.TITLE_KEY);
			subtitle = getIntent().getExtras().getString(DownloadActivity.SUBTITLE_KEY);
			int finId = fileName.indexOf("/");
			productId = fileName.substring(0,finId);
			
			
		}
	}	
	private ServiceConnection mServiceConn= new ServiceConnection() {
	   @Override
	   public void onServiceDisconnected(ComponentName name) {
		   Log.d(TAG,"onServiceDisconnected");
	       mService = null;
	   }

	   @Override
	   public void onServiceConnected(ComponentName name, 
	      IBinder service) {
		   Log.d(TAG,"onServiceConnected");
	       mService = IInAppBillingService.Stub.asInterface(service);
			AsyncTask<String, String, String> task = new AsyncTask<String, String, String>() {
				Bundle skuDetails = null;
				@Override
				protected String doInBackground(String... params) {
					try {
						ArrayList<String> skuList = new ArrayList<String>();
						skuList.add(productId);
						Bundle querySkus = new Bundle();
						querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
						skuDetails = mService.getSkuDetails(3,getPackageName(), "inapp", querySkus);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					return null;
				}
				@Override
				protected void onPostExecute(String result) {
					int response = skuDetails.getInt("RESPONSE_CODE");
					if (response == 0) {
					   ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");						   
					   for (String thisResponse : responseList) {
					      JSONObject object = null;
					      String sku="";
					      String price = "";
					      try {
					    	  object = new JSONObject(thisResponse);
					    	  sku = object.getString("productId");
					    	  price = object.getString("price");
					      } catch (JSONException e) {
					    	  e.printStackTrace();
					      }
					      if (sku.equals(productId)){
					    	  productPrice = price;
					      }
					   }
					}
					setContentView(R.layout.billing_activity);
					buy = (Button)findViewById(R.id.billing_buy_button);
					subs = (Button)findViewById(R.id.billing_subs_button);
					cancel = (Button)findViewById(R.id.billing_cancel_button);
					//
					cancel.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});
					//
					if(productPrice == null){
						buy.setVisibility(View.GONE);
					} else {
						buy.setText(title+": "+productPrice);
						buy.setOnClickListener(getBuyOnClick());
					}						
					//
					String abonnement = getResources().getString(R.string.abonnement_wind);
					String year = getResources().getString(R.string.year);
					subs.setText("   "+abonnement+" 1 "+year+"   ");
					subs.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(MainMagazineActivity.REQUEST_SUBS);
							sendBroadcast(intent);
							finish();
						}
					});
					super.onPostExecute(result);
				}
				
			}.execute();
	   }
	};
	private static final int BILLING_RESPONSE_RESULT_OK = 0;	
	private static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	private static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	private static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 5;
	private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	
	class PurchaseTask extends AsyncTask<String, String, String>{
		Bundle buyIntentBundle = null;
		@Override
		protected String doInBackground(String... params) {
			try {
				buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
						   productId, "inapp", null);
			} catch (RemoteException e1) {
				Log.e(TAG,"Problem with getBuyIntent",e1);
			}
			return null;
		}
		protected void onPostExecute(String result) {
			int response = buyIntentBundle.getInt("RESPONSE_CODE");
			Log.d(TAG,"response = "+response);
			switch (response) {
			case BILLING_RESPONSE_RESULT_USER_CANCELED:
			case BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
			case BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
				Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG).show();
				return;
			}
			//
			if(response == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){
				DownloadFromTempURLTask download = new DownloadFromTempURLTask();
	            download.execute();
	            return;
			} else if(response == BILLING_RESPONSE_RESULT_OK){
				PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
				if(pendingIntent==null){
					Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG).show();
					return;
				}
				try {
					startIntentSenderForResult(pendingIntent.getIntentSender(),
							CALLBACK_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
							Integer.valueOf(0));
				} catch (SendIntentException e) {
					Log.e(TAG,"Problem with startIntentSenderForResult",e);
				}
			}
			super.onPostExecute(result);
		}
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		Log.d(TAG,requestCode+" "+resultCode);
	   if (requestCode == CALLBACK_CODE) {    	
	      int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
	      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
	      String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
	        
	      if (resultCode == RESULT_OK&purchaseData!=null) {
	         try {
	        	 Log.d(TAG,"Succes!!!");
	            JSONObject jo = new JSONObject(purchaseData);
	            String sku = jo.getString("productId");
	            Log.d(TAG,"You have bought the " + sku + ". Excellent choice,adventurer!");
	            /*DownloadFromTempURLTask download = new DownloadFromTempURLTask();
	            download.execute();*/
	          }
	          catch (JSONException e) {
	             Log.d(TAG,"Failed to parse purchase data.");
	             e.printStackTrace();
	          }
	      } 
	   } 
	   finish();
	}
	
	private static final int CONNECTION_ALERT = 1;
	private static final int SERVER_ALERT = 2;
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECTION_ALERT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getResources().getString(R.string.connection_failed);
			builder.setMessage(message).setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			return builder.create();
		}
		case SERVER_ALERT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getResources().getString(R.string.server_error);
			builder.setMessage(message).setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//finish();
				}
			});
			return builder.create();
		}
		}
		return super.onCreateDialog(id);
	}
	
	
	@Override
	protected void onDestroy() {
		if (isNetworkConnected()) {
			unbindService(mServiceConn);
		}
		super.onDestroy();
	}
	
	private OnClickListener getBuyOnClick(){
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				PurchaseTask task = new PurchaseTask();
				task.execute();
				//finish();
			}
		};
	}

	private boolean isNetworkConnected() {
		return LibrelioApplication.thereIsConnection(this);
	}

	class DownloadFromTempURLTask extends AsyncTask<Void, Void, Void>{
		HttpResponse res = null;
		@Override
		protected Void doInBackground(Void... params) {
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(serverURL+"?product_id="+productId+"&code=HFGKEBNMVUKKEBFPOLJOIMKN34");
			HttpParams params1 = httpclient.getParams();
			HttpClientParams.setRedirecting(params1, false);
			try {
				res = httpclient.execute(httpget);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(Void result) {
			String tempURL = null;
			for(Header h : res.getAllHeaders()){
				if(h.getName().equalsIgnoreCase("location")){
					tempURL = h.getValue();
				}
                Log.d(TAG,"res- name:"+h.getName()+"  val:"+h.getValue());
			}
			if(tempURL==null){
				//showDialog(SERVER_ALERT);
				return;
			}
            //
            Intent intent = new Intent(getContext(),DownloadActivity.class);
            intent.putExtra(DownloadActivity.FILE_NAME_KEY,fileName);
            intent.putExtra(DownloadActivity.SUBTITLE_KEY,subtitle);
            intent.putExtra(DownloadActivity.TITLE_KEY,title);
            intent.putExtra(DownloadActivity.IS_TEMP_KEY, true);
            intent.putExtra(DownloadActivity.IS_SAMPLE_KEY, false);
            intent.putExtra(DownloadActivity.TEMP_URL_KEY, tempURL);
            startActivity(intent);
		};
	}
}
