package com.librelio.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.SkuDetails;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.librelio.LibrelioApplication;
import com.librelio.activity.BillingActivity;
import com.librelio.event.DownloadStatusUpdateEvent;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.service.MagazineDownloadService;
import com.librelio.storage.DownloadsManager;
import com.librelio.utils.CommonHelper;
import com.librelio.utils.PurchaseUtils;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FilenameUtils;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MagazineGridItemView extends FrameLayout {

    public static final int STANDARD = 0;
    public static final int HEADER = 1;
    public static final int DIALOG = 2;

    private final Context context;
    private final String plistName;
    private MagazineItem magazine;

    private final Button unitPrice;
    private final Button monthlySubscriptionPrice;
    private final Button yearlySubscriptionPrice;
    private final Button loginButton;
    private final TextView title;
    private final TextView subtitle;
    private final ImageView image;
    private final ImageView newsstandThumbnail;
    private Button sampleButton;
    private Button downloadButton;
    private final FrameLayout adLayout;
    private PublisherAdView adView;

    public MagazineGridItemView(Context context, int type, String plistName) {
        super(context);
        this.context = context;
        this.plistName = plistName;

        if (type == HEADER) {
            inflate(context, R.layout.item_dictitem_grid_header, this);
        } else if (type == DIALOG) {
            inflate(context, R.layout.item_dictitem_pop_up_dialog, this);
        } else {
            inflate(context, R.layout.item_dictitem_grid, this);
        }

        this.title = (TextView) findViewById(R.id.tag_title);
        this.subtitle = (TextView) findViewById(R.id.tag_subtitle);
        this.image = (ImageView) findViewById(R.id.tag_image);
        this.newsstandThumbnail = (ImageView) findViewById(R.id.tag_newsstand_cover);
        this.unitPrice = (Button) findViewById(R.id.tag_unit_price);
        this.monthlySubscriptionPrice = (Button) findViewById(R.id.tag_monthly_subscription_price);
        this.yearlySubscriptionPrice = (Button) findViewById(R.id.tag_yearly_subscription_price);
        this.sampleButton = (Button) findViewById(R.id.tag_sample);
        this.downloadButton = (Button) findViewById(R.id.tag_download);
        this.adLayout = (FrameLayout) findViewById(R.id.tag_ad);
        this.loginButton = (Button) findViewById(R.id.tag_login);
    }

    public void setMagazine(MagazineItem magazine) {
        this.magazine = magazine;
        setTag(magazine.getItemUrl());
        displayMagazineDetails();
    }

    private void displayMagazineDetails() {

        if (image != null) {
            Picasso.with(context).load(magazine.getPngUri()).fit().centerInside().placeholder(R.drawable
                    .generic)
                    .into(image);
            if (context.getResources().getBoolean(R.bool.enable_image_click_dialogs)) {
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MagazineGridItemView customView = new MagazineGridItemView(context, DIALOG,
                                plistName);
                        boolean wrapInScrollView = false;
                        MaterialDialog dialog = new MaterialDialog.Builder(context)
                                .customView(customView, wrapInScrollView)
                                .build();
                        dialog.show();
                        customView.setMagazine(magazine);
                    }
                });
            }
        }

        if (newsstandThumbnail != null) {
            Picasso.with(context).load(magazine.getNewsstandPngUri()).fit().centerInside().placeholder(R.drawable.generic)
                    .into(newsstandThumbnail);
        }

        if (title != null) {
            title.setText(magazine.getTitle());
        }
        if (subtitle != null) {
            subtitle.setText(magazine.getSubtitle());
        }

        // FIXME ad flickers when Plist is updated
        if (adLayout != null) {
            if (adView == null) {
                String string = context.getString(R.string.dfp_prefix);
                if (TextUtils.isEmpty(string)) {

                } else {
                    adView = new PublisherAdView(context);
                    String baseName = FilenameUtils.getBaseName(plistName);
                    adView.setAdUnitId(string + baseName);
                    int width = (int) CommonHelper.convertPixelsToDp(context.getResources()
                            .getDimension(R.dimen.header_ad_width), context);
                    int height = (int) CommonHelper.convertPixelsToDp(context.getResources().getDimension(R.dimen.header_ad_height), context);

                    adView.setAdSizes(new AdSize(width, height));
                    PublisherAdRequest adRequest = new PublisherAdRequest.Builder().build();
                    adView.loadAd(adRequest);
                }
                adLayout.addView(adView);
            }
        }

        if (unitPrice != null) {
            unitPrice.setVisibility(View.INVISIBLE);

            if (!magazine.isDownloaded()) {
                unitPrice.setText("");

                final Observable<SkuDetails> skuDetailsObservable = Observable.create(new Observable.OnSubscribe<SkuDetails>() {

                    @Override
                    public void call(Subscriber<? super SkuDetails> subscriber) {
                        SkuDetails magazineSkuDetails = LibrelioApplication.get()
                                .getBillingProcessor()
                                .getPurchaseListingDetails(magazine.getInAppBillingProductId());
                        if (magazineSkuDetails != null) {
                            subscriber.onNext(magazineSkuDetails);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new Throwable("No sku details"));
                        }

                    }
                });

                skuDetailsObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<SkuDetails>() {
                            @Override
                            public void call(SkuDetails skuDetails) {
                                if (magazine.getItemUrl().equals(getTag())) {
                                    unitPrice.setVisibility(View.VISIBLE);
                                    unitPrice.setText(skuDetails.priceText);
                                    unitPrice.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            BillingActivity.startActivityWithDialog(context,
                                                    BillingActivity
                                                            .SHOW_INDIVIDUAL_PURCHASE_DIALOG,
                                                    magazine);
                                        }
                                    });
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                unitPrice.setText(" --- ");
                                unitPrice.setOnClickListener(null);
                            }
                        });
            }
        }

        if (monthlySubscriptionPrice != null) {
            if (magazine.isDownloaded()) {
                monthlySubscriptionPrice.setVisibility(View.INVISIBLE);
            } else {

                monthlySubscriptionPrice.setText("");

                final Observable<SkuDetails> skuDetailsObservable = Observable.create(new Observable.OnSubscribe<SkuDetails>() {

                    @Override
                    public void call(Subscriber<? super SkuDetails> subscriber) {
                        SkuDetails magazineSkuDetails = LibrelioApplication.get()
                                .getBillingProcessor()
                                .getSubscriptionListingDetails(context.getString(R.string
                                        .monthly_subs_code));
                        if (magazineSkuDetails != null) {
                            subscriber.onNext(magazineSkuDetails);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new Throwable("No sku details"));
                        }

                    }
                });

                skuDetailsObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<SkuDetails>() {
                            @Override
                            public void call(SkuDetails skuDetails) {
                                if (monthlySubscriptionPrice != null) {
                                    yearlySubscriptionPrice.setVisibility(View.VISIBLE);
                                    monthlySubscriptionPrice.setText(PurchaseUtils
                                            .getFormattedPriceForButton(skuDetails.title,
                                                    skuDetails.priceText));
                                    monthlySubscriptionPrice.setOnClickListener(new OnClickListener() {

                                        @Override
                                        public void onClick(View v) {
                                            BillingActivity.startActivityWithDialog(context,
                                                    BillingActivity
                                                            .SHOW_MONTHLY_SUBSCRIPTION_DIALOG,
                                                    magazine);
                                        }
                                    });

                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                monthlySubscriptionPrice.setText(" --- ");
                            }
                        });
            }
        }

        if (yearlySubscriptionPrice != null) {
            if (magazine.isDownloaded()) {
                yearlySubscriptionPrice.setVisibility(View.INVISIBLE);
            } else {
                yearlySubscriptionPrice.setText("");

                final Observable<SkuDetails> skuDetailsObservable = Observable.create(new Observable.OnSubscribe<SkuDetails>() {

                    @Override
                    public void call(Subscriber<? super SkuDetails> subscriber) {
                        SkuDetails magazineSkuDetails = LibrelioApplication.get()
                                .getBillingProcessor()
                                .getSubscriptionListingDetails(context.getString(R.string
                                        .yearly_subs_code));
                        if (magazineSkuDetails != null) {
                            subscriber.onNext(magazineSkuDetails);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new Throwable("No sku details"));
                        }

                    }
                });

                skuDetailsObservable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<SkuDetails>() {
                            @Override
                            public void call(SkuDetails skuDetails) {
                                if (yearlySubscriptionPrice != null) {
                                    yearlySubscriptionPrice.setVisibility(View.VISIBLE);
                                    yearlySubscriptionPrice.setText(PurchaseUtils
                                            .getFormattedPriceForButton(skuDetails.title, skuDetails
                                                    .priceText));
                                    yearlySubscriptionPrice.setOnClickListener(new OnClickListener() {

                                        @Override
                                        public void onClick(View v) {
                                            BillingActivity.startActivityWithDialog(context,
                                                    BillingActivity.SHOW_YEARLY_SUBSCRIPTION_DIALOG,
                                                    magazine);
                                        }
                                    });
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                yearlySubscriptionPrice.setText(" --- ");
                            }
                        });
            }
        }

        if (loginButton != null) {
            if (!TextUtils.isEmpty(PurchaseUtils.getSavedUsername(context))) {
                loginButton.setVisibility(View.INVISIBLE);
            } else {
                loginButton.setVisibility(View.VISIBLE);
                loginButton.setText(R.string.deja_abonne);
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BillingActivity.startActivityWithDialog(context, BillingActivity
                                        .SHOW_USERNAME_PASSWORD_SUBSCRIPTION_DIALOG, magazine);
                    }
                });
            }
        }

        updateDownloadStatusOnButtons();
    }

    private void updateDownloadStatusOnButtons() {
        if (downloadButton != null) {
            setupDownloadButton();
        }

        if (sampleButton != null) {
            setupSampleButton();
        }
    }

    private void setupDownloadButton() {

        Pair<Integer, Boolean> downloadStatus = magazine.getDownloadStatus();

        Boolean isSampleDownloading = downloadStatus.second;
        Integer downloadStatusCode = downloadStatus.first;

        if (magazine.isDownloaded()) {
            downloadButton.setText(context.getString(R.string.read));
        } else if (!isSampleDownloading) { // is a sample downloading
            if (downloadStatusCode == DownloadStatusCode.QUEUED) {
                downloadButton.setText(context.getString(R.string.queued));
            } else if (downloadStatusCode == DownloadStatusCode.FAILED) {
                downloadButton.setText(context.getString(R.string.download_failed));
            } else if (downloadStatusCode > DownloadStatusCode.QUEUED
                    && downloadStatusCode < DownloadStatusCode.DOWNLOADED) {
                downloadButton.setText(String.valueOf(downloadStatusCode + "%"));
            } else {
                downloadButton.setText(R.string.download);
            }
        }

        if (magazine.isDownloaded()) {
            downloadButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LibrelioApplication.startPDFActivity(context, magazine.getItemFilePath(),
                            magazine.getTitle(), true);
                }
            });
        } else if (!isSampleDownloading) { // is a sample downloading
            if (downloadStatusCode >= DownloadStatusCode.QUEUED
                    && downloadStatusCode < DownloadStatusCode.DOWNLOADED) {
                downloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new MaterialDialog.Builder(context)
                                .title(R.string.cancel_download_question)
                                .positiveText(R.string.cancel)
                                .positiveColor(Color.BLACK)
                                .negativeText(R.string.continue_download)
                                .negativeColor(Color.BLACK)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        super.onPositive(dialog);
                                        // FIXME Cancel download
                                        DownloadsManager.removeDownload(context, magazine);
                                        magazine.clearMagazineDir(context);
                                        EventBus.getDefault().post(new DownloadStatusUpdateEvent());
                                    }
                                }).show();
                    }
                });
            } else {
                // not downloaded or download failed
                downloadButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startPaidDownload();
                    }
                });
            }
        } else {
            downloadButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPaidDownload();
                }
            });
        }
    }

    private void startPaidDownload() {
        BillingActivity.startActivityWithMagazine(context, magazine);
        downloadButton.setText("...");
    }

    private void setupSampleButton() {

        Pair<Integer, Boolean> downloadStatus = magazine.getDownloadStatus();

        Boolean isSampleDownloading = downloadStatus.second;
        Integer downloadStatusCode = downloadStatus.first;

        if (magazine.isDownloaded() ||
                (!isSampleDownloading && downloadStatusCode >= DownloadStatusCode.QUEUED &&
                        downloadStatusCode <= DownloadStatusCode.DOWNLOADED)) {
            // if magazine is downloaded or downloading
            sampleButton.setVisibility(View.INVISIBLE);
            return;
        } else {
            sampleButton.setVisibility(View.VISIBLE);
        }

        if (magazine.isSampleDownloaded()) {
            sampleButton.setText(context.getString(R.string.read_sample));
        } else if (isSampleDownloading) { // is a sample downloading
            if (downloadStatusCode == DownloadStatusCode.QUEUED) {
                sampleButton.setText(context.getString(R.string.queued));
            } else if (downloadStatusCode == DownloadStatusCode.FAILED) {
                sampleButton.setText(context.getString(R.string.download_failed));
            } else if (downloadStatusCode > DownloadStatusCode.QUEUED
                    && downloadStatusCode < DownloadStatusCode.DOWNLOADED) {
                sampleButton.setText(String.valueOf(downloadStatusCode + "%"));
            }
        } else {
            sampleButton.setText(R.string.sample);
        }

        if (magazine.isSampleDownloaded()) {
            sampleButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    LibrelioApplication.startPDFActivity(context,
                            magazine.getSamplePdfPath(), magazine.getTitle(), true);
                }
            });
        } else if (isSampleDownloading) { // is a sample downloading
            if (downloadStatusCode >= DownloadStatusCode.QUEUED
                    && downloadStatusCode < DownloadStatusCode.DOWNLOADED) {
                sampleButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new MaterialDialog.Builder(context)
                                .title(R.string.cancel_download_question)
                                .positiveText(R.string.cancel)
                                .positiveColor(Color.BLACK)
                                .negativeText(R.string.continue_download)
                                .negativeColor(Color.BLACK)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        super.onPositive(dialog);
                                        // FIXME Cancel download
                                        DownloadsManager.removeDownload(context, magazine);
                                        magazine.clearMagazineDir(context);
                                        EventBus.getDefault().post(new DownloadStatusUpdateEvent());
                                    }
                                }).show();
                    }
                });
            } else {
                // if download failed
                sampleButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MagazineDownloadService.startMagazineDownload(context, magazine, true);
                    }
                });
            }
        } else {
            sampleButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    MagazineDownloadService.startMagazineDownload(context, magazine, true);
                }
            });
        }
    }

    public void onEventMainThread(DownloadStatusUpdateEvent event) {
        updateDownloadStatusOnButtons();
    }

    public void onEventMainThread(ReloadPlistEvent event) {
        displayMagazineDetails();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        EventBus.getDefault().unregister(this);
        super.onDetachedFromWindow();
    }
}
