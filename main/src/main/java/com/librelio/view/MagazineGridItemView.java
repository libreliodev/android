package com.librelio.view;

import android.content.Context;
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
import com.librelio.utils.CommonHelper;
import com.librelio.utils.InAppBillingUtils;
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
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MagazineGridItemView customView = new MagazineGridItemView(context, DIALOG,
                            plistName);
                    customView.setMagazine(magazine);
                    boolean wrapInScrollView = false;
                    MaterialDialog dialog = new MaterialDialog.Builder(context)
                            .customView(customView, wrapInScrollView)
                            .build();
                    dialog.show();
                }
            });
        }

        if (newsstandThumbnail != null) {
            Picasso.with(context).load(magazine.getNewsstandPngUri()).fit().centerInside().placeholder(R.drawable.generic)
                    .into(newsstandThumbnail);
            newsstandThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
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
                            .getDimension(R.dimen
                                    .header_ad_width), context);
                    int height = (int) CommonHelper.convertPixelsToDp(context.getResources().getDimension(R.dimen.header_ad_height), context);

                    adView.setAdSizes(new AdSize(width, height));
                    PublisherAdRequest adRequest = new PublisherAdRequest.Builder()
                            .build();
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
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                unitPrice.setText(" --- ");
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
                                    monthlySubscriptionPrice.setText(InAppBillingUtils
                                            .getFormattedPriceForButton(skuDetails.title,
                                                    skuDetails.priceText));
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
                                    yearlySubscriptionPrice.setText(InAppBillingUtils
                                            .getFormattedPriceForButton(skuDetails.title, skuDetails
                                                    .priceText));
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
            if (!TextUtils.isEmpty(BillingActivity.getSavedUsername(context))) {
                loginButton.setVisibility(View.INVISIBLE);
            } else {
                loginButton.setVisibility(View.VISIBLE);
                loginButton.setText(R.string.deja_abonne);
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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

        setupDownloadedButtonText();

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (magazine.isDownloaded()) {
                    LibrelioApplication.startPDFActivity(
                            context,
                            magazine.getItemFilePath(),
                            magazine.getTitle(), true);
                } else {
//						MagazineDownloadService
//								.startMagazineDownload(context,
//										magazine, false);
                    BillingActivity.startActivityWithMagazine(context, magazine);
                    downloadButton.setText("...");
                }
            }
        });
    }

    private void setupSampleButton() {
        if (magazine.isDownloaded()) {
            sampleButton.setVisibility(View.INVISIBLE);
            return;
        }

        sampleButton.setVisibility(View.VISIBLE);

        setupSampleButtonText();
        sampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (magazine.isSampleDownloaded()) {
                    LibrelioApplication.startPDFActivity(
                            context,
                            magazine.getSamplePdfPath(),
                            magazine.getTitle(), true);
                } else {
                    MagazineDownloadService
                            .startMagazineDownload(context,
                                    magazine, true);
                }
            }
        });
    }

    private void setupSampleButtonText() {
        sampleButton.setText(context.getString(R.string.sample));
        Pair<Integer, Boolean> downloadStatus = magazine.getDownloadStatus();
        if (magazine.isSampleDownloaded()) {
            sampleButton.setText(context.getString(R.string.read_sample));
        } else if (downloadStatus.second) { // is a sample
            if (downloadStatus.first == DownloadStatusCode.QUEUED) {
                sampleButton.setText(context.getString(R.string.queued));
            } else if (downloadStatus.first == DownloadStatusCode.FAILED) {
                sampleButton.setText(context.getString(R.string.download_failed));
            } else if (downloadStatus.first >= 0
                    && downloadStatus.first <= 100) {
                sampleButton.setText(String.valueOf(downloadStatus.first + "%"));
            }
        }

        if (!downloadStatus.second
                && downloadStatus.first >= DownloadStatusCode.QUEUED
                && downloadStatus.first <= DownloadStatusCode.DOWNLOADED) {
            sampleButton.setVisibility(View.INVISIBLE);
        } else {
            sampleButton.setVisibility(View.VISIBLE);
        }
    }

    private void setupDownloadedButtonText() {
        downloadButton.setText(context.getString(R.string.download));
        Pair<Integer, Boolean> downloadStatus = magazine.getDownloadStatus();
        if (magazine.isDownloaded()) {
            downloadButton.setText(context.getString(R.string.read));
        } else if (!downloadStatus.second) { // is not a sample
            if (downloadStatus.first == DownloadStatusCode.QUEUED) {
                downloadButton.setText(context.getString(R.string.queued));
            } else if (downloadStatus.first == DownloadStatusCode.FAILED) {
                downloadButton.setText(context.getString(R.string.download_failed));
            } else if (downloadStatus.first >= 0
                    && downloadStatus.first <= 100) {
                downloadButton.setText(String.valueOf(downloadStatus.first + "%"));
            }
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
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }
}
