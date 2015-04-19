package com.librelio.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import com.librelio.activity.BillingActivity;

public class PurchaseUtils {

    public static SpannableString getFormattedPriceForButton(String title, String
            price) {
        SpannableString priceText = new SpannableString(price + "\n" + title);
        priceText.setSpan(new RelativeSizeSpan(0.7f), price.length() + 1, priceText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return priceText;
    }

    public static String getSavedSubscriberCode(Context context) {
        return context.getSharedPreferences(BillingActivity.SUBSCRIPTION_PREF, Context.MODE_PRIVATE)
                .getString(BillingActivity.PARAM_SUBSCRIPTION_CODE, null);
    }

    public static String getSavedUsername(Context context) {
        return context.getSharedPreferences(BillingActivity.SUBSCRIPTION_PREF, Context.MODE_PRIVATE)
                .getString(BillingActivity.PARAM_USERNAME, null);
    }

    public static String getSavedPassword(Context context) {
        return context.getSharedPreferences(BillingActivity.SUBSCRIPTION_PREF, Context.MODE_PRIVATE)
                .getString(BillingActivity.PARAM_PASSWORD, null);
    }
}
