package com.librelio.activity;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.android.vending.billing.IInAppBillingService;
import com.librelio.LibrelioApplication;
import com.librelio.base.BaseActivity;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.service.MagazineDownloadService;
import com.librelio.utils.PurchaseUtils;
import com.librelio.view.SubscriberCodeDialog;
import com.librelio.view.UsernamePasswordLoginDialog;
import com.niveales.wind.R;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;

public class BillingActivity extends BaseActivity {
    public static final String FILE_NAME_KEY = "file_name_key";
    public static final String TITLE_KEY = "title_key";
    public static final String SUBTITLE_KEY = "subtitle_key";
    public static final String IS_SAMPLE_KEY = "is_sample_key";
    private static final String TAG = "BillingActivity";

    // Only for test. Must always be FALSE!
    private static final boolean TEST_MODE = false;
    /*
     * productId can be the following values: android.test.purchased
     * android.test.canceled android.test.refunded android.test.item_unavailable
     */
    public static final String SUBSCRIPTION_PREF = "SubscriptionPreferences";
    public static final String PARAM_SUBSCRIPTION_CODE = "PARAM_SUBSCRIPTION_CODE";

    private static final String TEST_PRODUCT_ID = "android.test.purchased";

    private static final String PARAM_PRODUCT_ID = "@product_id";
    private static final String PARAM_DATA = "@data";
    private static final String PARAM_SIGNATURE = "@signature";
    private static final String PARAM_URLSTRING = "@urlstring";

    public static final String PARAM_USERNAME = "@username";
    public static final String PARAM_PASSWORD = "@password";
    private static final String PARAM_CODE = "@code";
    private static final String PARAM_CLIENT = "@client";
    private static final String PARAM_APP = "@app";
    private static final String PARAM_SERVICE = "@service";

    private static final String PARAM_DEVICEID = "@deviceid";

    private static final int CALLBACK_CODE = 101;
    private static final int UNAUTHORIZED_CODE = 401;
    private static final int UNAUTHORIZED_USER = 461;
    private static final int UNAUTHORIZED_ISSUE = 462;
    private static final int UNAUTHORIZED_DEVICE = 463;

    private static final int BILLING_RESPONSE_RESULT_OK = 0;
    private static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
    private static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    private static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 5;
    private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;

    public static final String SHOW_USERNAME_PASSWORD_SUBSCRIPTION_DIALOG
            = "show_username_password_subscription_dialog";
    public static final String SHOW_MONTHLY_SUBSCRIPTION_DIALOG
            = "show_monthly_subscription_dialog";
    public static final String SHOW_YEARLY_SUBSCRIPTION_DIALOG
            = "show_yearly_subscription_dialog";
    public static final String SHOW_INDIVIDUAL_PURCHASE_DIALOG
            = "show_individual_purchase_dialog";

    private String fileName;
    private String title;
    private String subtitle;
    private String productId;
    private String productPrice;
    private String productTitle;
    private String yearlySubPrice;
    private String yearlySubTitle;
    private String monthlySubPrice;
    private String monthlySubTitle;

    private Button buy;
    private Button cancel;
    private Button subsYear;
    private Button subsMonthly;
    private Button subsCode;
    private Button usernamePasswordLogin;

    private String ownedItemSignature = "";
    private String ownedItemPurshaseData = "";

    private IInAppBillingService billingService;
    private CircularProgressBar progress;
    private View content;

    public static void startActivityWithMagazine(Context context, MagazineItem item) {
        Intent intent = getIntent(context, item);
        context.startActivity(intent);
    }

    public static void startActivityWithDialog(Context context, String dialogType,
                                                                           MagazineItem item) {
        Intent intent = getIntent(context, item);
        intent.setAction(dialogType);
        context.startActivity(intent);
    }

    private static Intent getIntent(Context context, MagazineItem item) {
        Intent intent = new Intent(context,
                BillingActivity.class);
        intent.putExtra(FILE_NAME_KEY, item.getFilePath());
        intent.putExtra(TITLE_KEY, item.getTitle());
        intent.putExtra(SUBTITLE_KEY, item.getSubtitle());
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.billing_activity);

        content = findViewById(R.id.content);
        progress = (CircularProgressBar) findViewById(R.id.progress_bar);

        setResult(RESULT_CANCELED);

        if (!isNetworkConnected()) {
            showAlertDialog(CONNECTION_ALERT);
        } else {
            Intent iapIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            iapIntent.setPackage("com.android.vending");
            getContext().bindService(iapIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            if (getIntent().getExtras() != null) {
                fileName = getIntent().getExtras().getString(FILE_NAME_KEY);
                title = getIntent().getExtras().getString(TITLE_KEY);
                subtitle = getIntent().getExtras().getString(SUBTITLE_KEY);
                // Using Locale.US to avoid different results in different locales
                productId = FilenameUtils.getName(fileName).toLowerCase(Locale.US);
                productId = productId.substring(0, productId.indexOf("_.pdf"));
            }
        }
    }

    private void setupDialogView() {

        boolean includeSubscriptions = false;

        String baseName = FilenameUtils.getBaseName(fileName);

        String baseNameWithoutLastUnderscore = baseName.substring(0, baseName.length() - 1);

        if (baseNameWithoutLastUnderscore.contains("_")) {
            String dateString = baseNameWithoutLastUnderscore
                    .substring(baseNameWithoutLastUnderscore.indexOf("_") + 1);
            if (dateString.length() == 6) {
                DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
                DateTime date = DateTime.parse(dateString, formatter);
                if (date.isAfter(DateTime.now().minusMonths(1))) {
                    includeSubscriptions = true;
                }
            }
        }

        hideProgressBar();

        buy = (Button) findViewById(R.id.billing_buy_button);
        subsMonthly = (Button) findViewById(R.id.billing_subs_monthly);
        subsYear = (Button) findViewById(R.id.billing_subs_year);
        subsCode = (Button) findViewById(R.id.billing_subs_code_button);
        usernamePasswordLogin = (Button) findViewById(R.id.billing_username_password_login_button);
        cancel = (Button) findViewById(R.id.billing_cancel_button);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (productPrice == null) {
            buy.setVisibility(View.GONE);
        } else {
            buy.setText(productTitle + ": " + productPrice);
            buy.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    purchaseItem();
                }

            });
        }

        if (yearlySubPrice == null || !includeSubscriptions) {
            subsYear.setVisibility(View.GONE);
        } else {
            subsYear.setText(yearlySubTitle + ": " + yearlySubPrice);
            subsYear.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    purchaseYearlySub();
                }

            });
        }

        if (monthlySubPrice == null || !includeSubscriptions) {
            subsMonthly.setVisibility(View.GONE);
        } else {
            subsMonthly.setText(monthlySubTitle + ": " + monthlySubPrice);
            subsMonthly.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    purchaseMonthlySub();
                }

            });
        }

        if (LibrelioApplication.isEnableCodeSubs(getContext())) {
            subsCode.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSubscriberCodeDialog(false);
                }
            });
        } else {
            subsCode.setVisibility(View.GONE);
        }
        if (LibrelioApplication.isEnableUsernamePasswordLogin(getContext())) {
            usernamePasswordLogin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUsernamePasswordLoginDialog(false);
                }
            });
        } else {
            usernamePasswordLogin.setVisibility(View.GONE);
        }
    }

    // This is to check subscription status when a push notification is sent for
    // a new issue
    // If something goes wrong, don't download the issue, just show a
    // notification
    public static boolean backgroundCheckForValidSubscriptionFailFast(
            Context context, String fileName, String title, String subtitle) {

        String prefSubscrCode = PurchaseUtils.getSavedSubscriberCode(context);

        if (prefSubscrCode != null) {
            String subscriptionCodeQuery = buildSubscriptionCodeQuery(context,
                    prefSubscrCode, fileName);
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpGet httpget = new HttpGet(subscriptionCodeQuery);
                HttpClientParams.setRedirecting(httpclient.getParams(), false);
                HttpResponse httpResponse = httpclient.execute(httpget);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                if (responseCode == UNAUTHORIZED_CODE) {
                } else if (responseCode == UNAUTHORIZED_ISSUE) {
                } else if (responseCode == UNAUTHORIZED_DEVICE) {
                } else {
//                    String tempURL = getTempURL(httpResponse);
//                    if (tempURL != null) {
//                        MagazineDownloadService
//                                .startMagazineDownload(context, new MagazineItem(
//                                                context, title, subtitle, fileName),
//                                        true, tempURL, false);
//                        return true;
//                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "URI is malformed", e);
            } catch (ClientProtocolException e) {
                Log.e(TAG, "Download failed", e);
            } catch (IOException e) {
                Log.e(TAG, "Download failed", e);
            }
            return false;
        }

        String prefUsername = PurchaseUtils.getSavedUsername(context);

        if (prefUsername != null) {
            String prefPassword = PurchaseUtils.getSavedPassword(context);
            String usernamePasswordLoginQuery = buildUsernamePasswordLoginQuery(
                    context, prefUsername, prefPassword, fileName);
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpGet httpget = new HttpGet(usernamePasswordLoginQuery);
                HttpClientParams.setRedirecting(httpclient.getParams(), false);
                HttpResponse httpResponse = httpclient.execute(httpget);
                int responseCode = httpResponse.getStatusLine().getStatusCode();
                if (responseCode == UNAUTHORIZED_CODE) {
                } else if (responseCode == UNAUTHORIZED_ISSUE) {
                } else if (responseCode == UNAUTHORIZED_DEVICE) {
                } else {
//                    String tempURL = getTempURL(httpResponse);
//                    if (tempURL != null) {
//                        MagazineDownloadService
//                                .startMagazineDownload(context, new MagazineItem(
//                                                context, title, subtitle, fileName),
//                                        true, tempURL, false);
//                        return true;
//                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "URI is malformed", e);
            } catch (ClientProtocolException e) {
                Log.e(TAG, "Download failed", e);
            } catch (IOException e) {
                Log.e(TAG, "Download failed", e);
            }
            return false;
        }
        return false;
    }

    private boolean checkForValidSubscription(Context context, String fileName) {
        String prefSubscrCode = PurchaseUtils.getSavedSubscriberCode(context);

        if (prefSubscrCode != null) {
            downloadSubscriberCodeLoginFromTempURL(buildSubscriptionCodeQuery(context,
                    prefSubscrCode, fileName), prefSubscrCode);
            return true;
        }

        String prefUsername = PurchaseUtils.getSavedUsername(context);

        if (prefUsername != null) {
            String prefPassword = PurchaseUtils.getSavedPassword(context);
            downloadUsernamePasswordLoginFromTempURL(
                    buildUsernamePasswordLoginQuery(context,
                            prefUsername, prefPassword, fileName),
                    prefUsername, prefPassword);
            return true;
        }
        return false;
    }

    private void showSubscriberCodeDialog(boolean error) {
        SubscriberCodeDialog dialog = new SubscriberCodeDialog(getContext(),
                getString(R.string.please_enter_your_code), error);
        dialog.setSubscriberCodeListener(onSubscriberCodeListener);
        dialog.show();
    }

    private void showUsernamePasswordLoginDialog(boolean error) {
        UsernamePasswordLoginDialog dialog = new UsernamePasswordLoginDialog(
                getContext(),
                getString(R.string.please_enter_your_username_password_login),
                error);
        dialog.setOnUsernamePasswordLoginListener(onUsernamePasswordLoginListener);
        dialog.show();
    }

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            billingService = IInAppBillingService.Stub.asInterface(service);
            new AsyncTask<String, String, Bundle>() {
                private Bundle ownedItems = null;
                private Bundle ownedSubs = null;

                @Override
                protected Bundle doInBackground(String... params) {
                    Bundle skuDetails = null;
                    try {
                        ArrayList<String> skuList = new ArrayList<String>();
                        skuList.add(productId);

                        // Add subscription codes
                        skuList.add(LibrelioApplication
                                .getYearlySubsCode(getContext()));
                        skuList.add(LibrelioApplication
                                .getMonthlySubsCode(getContext()));

                        Bundle querySkus = new Bundle();
                        querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

                        // Retrieve relevant in app items
                        skuDetails = billingService.getSkuDetails(3,
                                getPackageName(), "inapp", querySkus);
                        ArrayList<String> details = skuDetails
                                .getStringArrayList("DETAILS_LIST");

                        // Retrieve relevant subscriptions
                        skuDetails = billingService.getSkuDetails(3,
                                getPackageName(), "subs", querySkus);
                        ArrayList<String> subsDetails = skuDetails
                                .getStringArrayList("DETAILS_LIST");

                        // Combine in app and subscriptions
                        details.addAll(subsDetails);
                        skuDetails.putStringArrayList("DETAILS_LIST", details);

                        // Retrieve owned in app items
                        ownedItems = billingService.getPurchases(3,
                                getPackageName(), "inapp", null);
                        // Retrieve owned AND current subscriptions
                        ownedSubs = billingService.getPurchases(3,
                                getPackageName(), "subs", null);

                    } catch (RemoteException e) {
                        Log.d(TAG, "InAppBillingService failed", e);
                        return null;
                    }
                    return skuDetails;
                }

                @Override
                protected void onPostExecute(Bundle skuDetails) {
                    // If item was purchase then download begin without open
                    // billing activity
                    int getPurchaseResponse = ownedItems
                            .getInt("RESPONSE_CODE");
                    if (TEST_MODE) {
                        getPurchaseResponse = -1;
                    }
                    if (getPurchaseResponse == 0) {
                        ArrayList<String> ownedSkus = ownedItems
                                .getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        for (String s : ownedSkus) {
                            Log.d(TAG, productId + " already purchased? " + s);
                        }
                        if (ownedSkus.contains(productId)) {
                            prepareDownloadWithOwnedItem(ownedItems, productId);
                            return;
                        }
                        ownedSkus = ownedSubs
                                .getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                        for (String s : ownedSkus) {
                            Log.d(TAG, productId + " already purchased? " + s);
                        }

                        if (ownedSkus.contains(LibrelioApplication
                                .getYearlySubsCode(getContext()))) {
                            prepareDownloadWithOwnedItem(ownedSubs,
                                    LibrelioApplication
                                            .getYearlySubsCode(getContext()));
                            return;
                        }
                        if (ownedSkus.contains(LibrelioApplication
                                .getMonthlySubsCode(getContext()))) {
                            prepareDownloadWithOwnedItem(ownedSubs,
                                    LibrelioApplication
                                            .getMonthlySubsCode(getContext()));
                            return;
                        }
                    }

                    int response = skuDetails.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        Log.d(TAG, "response code was success");
                        ArrayList<String> details = skuDetails
                                .getStringArrayList("DETAILS_LIST");
                        for (String detail : details) {
                            Log.d(TAG, "response = " + detail);
                            JSONObject object = null;
                            String sku = "";
                            String price = "";
                            String title = "";
                            try {
                                object = new JSONObject(detail);
                                sku = object.getString("productId");
                                price = object.getString("price");
                                title = object.getString("title");
                            } catch (JSONException e) {
                                Log.e(TAG, "getSKU details failed", e);
                            }
                            if (sku.equals(productId)) {
                                productPrice = price;
                                productTitle = title;
                            } else if (sku.equals(LibrelioApplication
                                    .getYearlySubsCode(getContext()))) {
                                yearlySubPrice = price;
                                yearlySubTitle = title;

                            } else if (sku.equals(LibrelioApplication
                                    .getMonthlySubsCode(getContext()))) {
                                monthlySubPrice = price;
                                monthlySubTitle = title;
                            }
                        }
                    }
                    if (!checkForValidSubscription(BillingActivity.this,
                            fileName)) {
                        if (SHOW_USERNAME_PASSWORD_SUBSCRIPTION_DIALOG.equals(getIntent().getAction())) {
                            showUsernamePasswordLoginDialog(false);
                        } else if (SHOW_MONTHLY_SUBSCRIPTION_DIALOG.equals(getIntent().getAction())) {
                            purchaseMonthlySub();
                        } else if (SHOW_YEARLY_SUBSCRIPTION_DIALOG.equals(getIntent().getAction())) {
                            purchaseYearlySub();
                        } else if (SHOW_INDIVIDUAL_PURCHASE_DIALOG.equals(getIntent().getAction())) {
                            purchaseItem();
                        } else {
                            setupDialogView();
                        }
                    }
                    super.onPostExecute(skuDetails);
                }

                protected void prepareDownloadWithOwnedItem(Bundle ownedBundle,
                                                            String subsoritemID) {
                    ArrayList<String> ownedSkus = ownedBundle
                            .getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    int idx = ownedSkus.indexOf(subsoritemID);
                    ArrayList<String> purchaseDataList = ownedBundle
                            .getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    ArrayList<String> signatureList = ownedBundle
                            .getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                    Log.d(TAG, "[getPurchases] purchaseDataList: "
                            + purchaseDataList);
                    Log.d(TAG, "[getPurchases] signatureList: " + signatureList);
                    if (purchaseDataList != null) {
                        ownedItemPurshaseData = purchaseDataList.get(idx);
                    }
                    if (signatureList != null) {
                        ownedItemSignature = signatureList.get(idx);
                    }
                    onDownloadAction(ownedItemPurshaseData, ownedItemSignature);
                    return;
                }
            }.execute();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, requestCode + " " + resultCode);
        Log.d(TAG, "data = "
                + data.getExtras().getString("INAPP_PURCHASE_DATA"));
        Log.d(TAG,
                "signature = "
                        + data.getExtras().getString("INAPP_DATA_SIGNATURE"));

        if (requestCode == CALLBACK_CODE) {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            if (resultCode == RESULT_OK && purchaseData != null) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    String dataResponse = data.getExtras().getString(
                            "INAPP_PURCHASE_DATA");
                    String signatureResponse = data.getExtras().getString(
                            "INAPP_DATA_SIGNATURE");
                    Log.d(TAG, "You have bought the " + sku
                            + ". Excellent choice, adventurer!");
                    if (getIntent().getExtras() != null) {
                        onDownloadAction(dataResponse, signatureResponse);
                    } else {
                        Toast.makeText(this, "Purchase successful",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse purchase data.", e);
                }
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (isNetworkConnected()) {
            unbindService(mServiceConn);
        }
        super.onDestroy();
    }

    protected void onDownloadAction(String dataResponse,
                                    String signatureResponse) {
        String url = buildVerifyQuery(dataResponse, signatureResponse);

        Request request = new Request.Builder()
                .url(url)
                .build();

        LibrelioApplication.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                }
            });
            }

            @Override
            public void onResponse(Response response) throws IOException {
                startDownloadOfMagazineFromResponse(response);
            }
        });
    }

    private void purchaseItem() {
        new PurchaseTask().execute(productId);
    }

    private void purchaseMonthlySub() {
        new PurchaseTask().execute(LibrelioApplication.getMonthlySubsCode(getContext()));
    }

    private void purchaseYearlySub() {
        new PurchaseTask().execute(LibrelioApplication.getYearlySubsCode(getContext()));
    }

    private boolean isNetworkConnected() {
        return LibrelioApplication.thereIsConnection(this);
    }

    private Context getContext() {
        return this;
    }

    private String buildVerifyQuery(String dataResponse, String signatureResponse) {

        StringBuilder query = new StringBuilder(
                LibrelioApplication.getServerUrl(getContext()));

        String command = getString(R.string.command_android_verify)
                .replace(";", "&")
                .replace(PARAM_PRODUCT_ID, Uri.encode(productId))
                .replace(PARAM_DATA, Uri.encode(dataResponse))
                .replace(PARAM_SIGNATURE, Uri.encode(signatureResponse))
                .replace(PARAM_URLSTRING,
                        Uri.encode(LibrelioApplication.getUrlString(getContext(), fileName)));

        return query.append(command).toString();
    }

    private static String buildSubscriptionCodeQuery(Context context,
                                                     String code, String fileName) {

        StringBuilder query = new StringBuilder(LibrelioApplication.getServerUrl(context));

        String command = context.getString(R.string.command_pswd)
                .replace(PARAM_CODE, Uri.encode(code))
                .replace(PARAM_URLSTRING, Uri.encode(LibrelioApplication.getUrlString(fileName)))
                .replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(context)))
                .replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(context)))
                .replace(PARAM_DEVICEID, LibrelioApplication.getAndroidId(context));
        return query.append(command).toString();
    }

    private static String buildUsernamePasswordLoginQuery(Context context, String username,
                                                          String password, String fileName) {

        StringBuilder query = new StringBuilder(LibrelioApplication.getServerUrl(context));

        String command = context.getString(R.string.command_username_pswd)
                .replace(PARAM_URLSTRING, Uri.encode(LibrelioApplication.getUrlString(fileName)))
                .replace(PARAM_USERNAME, Uri.encode(username))
                .replace(PARAM_PASSWORD, Uri.encode(password))
                .replace(PARAM_CLIENT, Uri.encode(LibrelioApplication.getClientName(context)))
                .replace(PARAM_APP, Uri.encode(LibrelioApplication.getMagazineName(context)))
                .replace(PARAM_SERVICE, Uri.encode(LibrelioApplication.getServiceName(context)))
                .replace(PARAM_DEVICEID, LibrelioApplication.getAndroidId(context));
        return query.append(command).toString();
    }

    private void startDownloadOfMagazineFromResponse(Response response) {
        String tempURL = null;
        if (null == response) {
            showAlertDialog(DOWNLOAD_ALERT);
            Log.w(TAG, "download response was null");
            return;
        }

        tempURL = getTempURL(response);
        if (tempURL == null) {
            showAlertDialog(DOWNLOAD_ALERT);
            return;
        }
        if (getIntent().getExtras() != null) {
            MagazineDownloadService.startMagazineDownload(this, new MagazineItem(
                    this, title, subtitle, fileName), true, tempURL, false);
//            Intent intent = new Intent(getContext(),
//                    DownloadMagazineActivity.class);
//            intent.putExtra(BillingActivity.FILE_NAME_KEY, fileName);
//            intent.putExtra(BillingActivity.SUBTITLE_KEY, subtitle);
//            intent.putExtra(BillingActivity.TITLE_KEY, title);
//            startActivity(intent);
//            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Purchase successful", Toast.LENGTH_LONG)
                    .show();
        }
        finish();
    }

    private static String getTempURL(Response response) {
        if (null != response.headers()) {
            String location = response.header("location");
            return location;
        }
        return null;
    }

    private void downloadSubscriberCodeLoginFromTempURL(String url, final String subscriberCode) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        LibrelioApplication.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int responseCode = response.code();
                        if (responseCode == UNAUTHORIZED_USER) {
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE).edit()
                                    .remove(PARAM_SUBSCRIPTION_CODE).apply();
                            showSubscriberCodeDialog(true);
                        } else if (responseCode == UNAUTHORIZED_ISSUE) {
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.unauthorized_issue));
                            builder.setPositiveButton(R.string.buy,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            setupDialogView();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else if (responseCode == UNAUTHORIZED_DEVICE) {
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.unauthorized_device));
                            builder.setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            finish();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else if (responseCode == 500) {
                            // Server error
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE).edit()
                                    .remove(PARAM_SUBSCRIPTION_CODE).apply();
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.server_error));
                            builder.setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            setupDialogView();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else {
                            String prefSubscrCode = PurchaseUtils.getSavedSubscriberCode
                                    (BillingActivity.this);
                            if (prefSubscrCode == null) {
                                getContext()
                                        .getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE).edit()
                                        .putString(PARAM_SUBSCRIPTION_CODE, subscriberCode)
                                        .apply();
                            }
                            startDownloadOfMagazineFromResponse(response);
                        }
                    }
                });
            }
        });
    }

    private void downloadUsernamePasswordLoginFromTempURL(String url, final String username,
                                                          final String password) {

        Request request = new Request.Builder()
                .url(url)
                .build();

        LibrelioApplication.getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                }
            });
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int responseCode = response.code();
                        if (responseCode == UNAUTHORIZED_USER) {
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE)
                                    .edit()
                                    .remove(PARAM_USERNAME).apply();
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE)
                                    .edit()
                                    .remove(PARAM_PASSWORD).apply();
                            showUsernamePasswordLoginDialog(true);
                        } else if (responseCode == UNAUTHORIZED_ISSUE) {
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.unauthorized_issue));
                            builder.setPositiveButton(R.string.buy,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            setupDialogView();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else if (responseCode == UNAUTHORIZED_DEVICE) {
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.unauthorized_device));
                            builder.setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            finish();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else if (responseCode == 500) {
                            // Server error
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE)
                                    .edit()
                                    .remove(PARAM_USERNAME).apply();
                            getContext().getSharedPreferences(SUBSCRIPTION_PREF, MODE_PRIVATE)
                                    .edit()
                                    .remove(PARAM_PASSWORD).apply();
                            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(
                                    BillingActivity.this);
                            builder.setMessage(getString(R.string.server_error));
                            builder.setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            setupDialogView();
                                        }
                                    });
                            Dialog dialog = builder.create();
                            dialog.setOnCancelListener(new OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            });
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        } else {
                            String prefUsername = getContext().getSharedPreferences(
                                    SUBSCRIPTION_PREF, MODE_PRIVATE).getString(PARAM_USERNAME,
                                    null);
                            if (prefUsername == null) {
                                getContext().getSharedPreferences(SUBSCRIPTION_PREF,
                                        MODE_PRIVATE).edit()
                                        .putString(PARAM_USERNAME, username).apply();
                                getContext().getSharedPreferences(SUBSCRIPTION_PREF,
                                        MODE_PRIVATE).edit()
                                        .putString(PARAM_PASSWORD, password).apply();
                            }
                            startDownloadOfMagazineFromResponse(response);
                        }


                    }
                });
            }
        });
    }

    private class PurchaseTask extends AsyncTask<String, String, Bundle> {
        private Bundle ownedItems;

        @Override
        protected Bundle doInBackground(String... params) {
            String itemToBuyId = params[0];
            Log.d(TAG, "id: " + itemToBuyId);
            try {
                ownedItems = billingService.getPurchases(3, getPackageName(),
                        "inapp", null);
                if (TEST_MODE) {
                    productId = TEST_PRODUCT_ID;
                }
                if (itemToBuyId.equals(productId)) {
                    Log.d(TAG, "let us buy a product " + itemToBuyId);
                    return billingService.getBuyIntent(3, getPackageName(),
                            productId, "inapp", null);
                } else {
                    Log.d(TAG, "let us buy a subscription " + itemToBuyId);
                    return billingService.getBuyIntent(3, getPackageName(),
                            itemToBuyId, "subs", null);

                }

            } catch (RemoteException e) {
                Log.e(TAG, "Problem with getBuyIntent", e);
            }
            return null;
        }

        protected void onPostExecute(Bundle result) {
            super.onPostExecute(result);

            if (null == result) {
                return;
            }
            int response = result.getInt("RESPONSE_CODE");
            Log.d(TAG, "Purchase response = " + response);
            switch (response) {
                case BILLING_RESPONSE_RESULT_USER_CANCELED:
                case BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
                case BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
                    Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG)
                            .show();
                    return;
            }
            if (response == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                Log.d(TAG, productId + " ITEM_ALREADY_OWNED ");
                if (ownedItemPurshaseData != null & ownedItemSignature != null) {
                    if ((!ownedItemPurshaseData.equals(""))
                            & (!ownedItemSignature.equals(""))) {
                        onDownloadAction(ownedItemPurshaseData,
                                ownedItemSignature);
                    }
                }
                return;
            } else if (response == BILLING_RESPONSE_RESULT_OK) {
                PendingIntent pendingIntent = result
                        .getParcelable("BUY_INTENT");
                Log.d(TAG, "pendingIntent = " + pendingIntent);
                if (pendingIntent == null) {
                    Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            CALLBACK_CODE, new Intent(), 0, 0, 0);
                } catch (SendIntentException e) {
                    Log.e(TAG, "Problem with startIntentSenderForResult", e);
                }
            }
        }
    }

    private SubscriberCodeDialog.OnSubscriberCodeListener onSubscriberCodeListener = new SubscriberCodeDialog.OnSubscriberCodeListener() {
        @Override
        public void onEnterValue(String code) {
            showProgressBar();
            downloadSubscriberCodeLoginFromTempURL(buildSubscriptionCodeQuery
                    (BillingActivity.this, code, fileName), code);
        }

        @Override
        public void onCancel() {
            finish();
        }
    };

    private UsernamePasswordLoginDialog.OnUsernamePasswordLoginListener onUsernamePasswordLoginListener = new UsernamePasswordLoginDialog.OnUsernamePasswordLoginListener() {
        @Override
        public void onEnterUsernamePasswordLogin(String username, String password) {
            showProgressBar();
            downloadUsernamePasswordLoginFromTempURL(buildUsernamePasswordLoginQuery
                    (BillingActivity.this, username, password, fileName), username, password);
        }

        @Override
        public void onCancel() {
            finish();
        }
    };

    private void showProgressBar() {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.INVISIBLE);
    }

    private void hideProgressBar() {
        progress.setVisibility(View.INVISIBLE);
        content.setVisibility(View.VISIBLE);
    }

}
