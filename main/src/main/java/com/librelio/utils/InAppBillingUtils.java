package com.librelio.utils;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

public class InAppBillingUtils {

    public static SpannableString getFormattedPriceForButton(String title, String
            price) {
        SpannableString priceText = new SpannableString(price + "\n" + title);
        priceText.setSpan(new RelativeSizeSpan(0.7f), price.length() + 1, priceText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return priceText;
    }
}
