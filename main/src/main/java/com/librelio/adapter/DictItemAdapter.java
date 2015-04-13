package com.librelio.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.SkuDetails;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.librelio.LibrelioApplication;
import com.librelio.activity.BillingActivity;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.service.MagazineDownloadService;
import com.librelio.utils.CommonHelper;
import com.librelio.utils.InAppBillingUtils;
import com.librelio.view.EventBusButton;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class DictItemAdapter extends RecyclerView.Adapter {

	private static final int TYPE_HEADER = 2;
	private static final int TYPE_DEFAULT = 1;

	private final Context context;
	private final ArrayList<DictItem> dictItems;
	private String plistName;

	public DictItemAdapter(Context context, ArrayList<DictItem> dictItems, String plistName) {
		this.context = context;
		this.dictItems = dictItems;
		this.plistName = plistName;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_HEADER:
				final View header = LayoutInflater.from(context).inflate(R.layout
						.item_dictitem_grid_header, parent, false);
				return new DefaultViewHolder(context, header, plistName);
			case TYPE_DEFAULT:
				final View view = LayoutInflater.from(context).inflate(R.layout
						.item_dictitem_grid, parent, false);
				return new DefaultViewHolder(context, view, plistName);
		}
		return null;
	}

	@Override
	public int getItemViewType(int position) {
		if (position == 0) {
			return TYPE_HEADER;
		} else {
			return TYPE_DEFAULT;
		}
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
		((DefaultViewHolder) holder).bind(dictItems.get(position));
	}

	@Override
	public int getItemCount() {
		return dictItems.size();
	}

//    public void setProducts(Inventory.Products products) {
//        this.products = products;
//        notifyDataSetChanged();
//    }


	//TODO Remove duplicated code between default and header views


//    public static class DefaultViewHolder extends RecyclerView.ViewHolder {
//
//        public TextView title;
//        public TextView subtitle;
//        public ImageView image;
//        public LinearLayout progressLayout;
//        public TextView info;
//        public SmoothProgressBar progressBar;
//
//        public DefaultViewHolder(View view) {
//            super(view);
//            this.title = (TextView) view.findViewById(R.id.tag_title);
//            this.subtitle = (TextView) view.findViewById(R.id.tag_subtitle);
//            this.image = (ImageView) view
//                    .findViewById(R.id.item_thumbnail);
//            this.progressLayout = (LinearLayout) view
//                    .findViewById(R.id.item_progress_layout);
//            this.info = (TextView) view.findViewById(R.id.item_info);
//            this.progressBar = (SmoothProgressBar) view
//                    .findViewById(R.id.progress_bar);
//        }
//
//        public void bind(final Context context, final DictItem dictItem) {
//             reset the visibilities
//            if (title != null) {
//                title.setText("");
//            }
//            if (subtitle != null) {
//                subtitle.setText("");
//            }
//            progressLayout.setVisibility(View.GONE);
//            progressBar.setVisibility(View.GONE);
//            info.setVisibility(View.GONE);
//
//            final MagazineItem magazine = (MagazineItem) dictItem;
//
//            if (dictItem instanceof DisplayableAsGridItem) {
//                final DisplayableAsGridItem displayable = ((DisplayableAsGridItem) dictItem);
//                if (title != null) {
//                    title.setText(displayable.getTitle());
//                }
//                if (subtitle != null) {
//                    subtitle.setText(displayable.getSubtitle());
//                }
//                String pngUri = displayable.getPngUri();
//                Picasso.with(context).load(pngUri).fit().centerInside()
//                        .into(image);
//                image.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        displayable.onThumbnailClick(context);
//
//                    }
//                });
//            }
//
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (magazine.isDownloaded()) {
//                        LibrelioApplication.startPDFActivity(context, magazine, true);
//                    } else {
//                        BillingActivity.startActivityWithMagazine(context, (MagazineItem) dictItem);
//                    }
//                }
//            });
//        }
//    }

	public static class DefaultViewHolder extends RecyclerView.ViewHolder {

		private final Context context;
		private final Button unitPrice;
		private final Button monthlySubscriptionPrice;
		private final Button yearlySubscriptionPrice;
		private final String plistName;
		private final View view;
		private final Button loginButton;
		private TextView title;
		private TextView subtitle;
		private ImageView image;
		private final ImageView newsstandThumbnail;
		private LinearLayout progressLayout;
		private TextView info;
		private SmoothProgressBar progressBar;
		private Button sampleButton;
		private Button downloadButton;
		private final FrameLayout adLayout;
		private PublisherAdView adView;

		public DefaultViewHolder(Context context, View view, String plistName) {
			super(view);
			this.context = context;
			this.view = view;
			this.plistName = plistName;
			this.title = (TextView) view.findViewById(R.id.tag_title);
			this.subtitle = (TextView) view
					.findViewById(R.id.tag_subtitle);
			this.image = (ImageView) view
					.findViewById(R.id.tag_image);
			this.newsstandThumbnail = (ImageView) view
					.findViewById(R.id.tag_newsstand_cover);
			this.unitPrice = (Button) view.findViewById(R.id.tag_unit_price);
			this.monthlySubscriptionPrice = (Button) view.findViewById(R.id
					.tag_monthly_subscription_price);
			this.yearlySubscriptionPrice = (Button) view.findViewById(R.id
					.tag_yearly_subscription_price);
			this.sampleButton = (Button) view.findViewById(R.id.tag_sample);
			this.downloadButton = (Button) view.findViewById(R.id.tag_download);
			this.adLayout = (FrameLayout) view.findViewById(R.id.tag_ad);
			this.loginButton = (Button) view.findViewById(R.id.tag_login);
		}

		public void bind(final DictItem dictItem) {

			final MagazineItem magazine = (MagazineItem) dictItem;

			// Set tag
			view.setTag(magazine.getItemUrl());

			// reset the visibilities
			if (title != null) {
				title.setText("");
			}
			if (subtitle != null) {
				subtitle.setText("");
			}
//            progressLayout.setVisibility(View.GONE);
//            progressBar.setVisibility(View.GONE);
//            info.setVisibility(View.GONE);
//            readButton.setVisibility(View.GONE);
//            downloadButton.setVisibility(View.GONE);
//            deleteButton.setVisibility(View.GONE);
//            sampleButton.setVisibility(View.GONE);
//            cancelButton.setVisibility(View.GONE);

			if (dictItem instanceof DisplayableAsGridItem) {
				final DisplayableAsGridItem displayable = ((DisplayableAsGridItem) dictItem);
				if (title != null) {
					title.setText(displayable.getTitle());
				}
				if (subtitle != null) {
					subtitle.setText(displayable.getSubtitle());
				}
				if (image != null) {
					Picasso.with(context).load(displayable.getPngUri()).fit().centerInside().placeholder(R.drawable.generic)
							.into(image);
					image.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
//							displayable.onThumbnailClick(context);
							boolean wrapInScrollView = false;
							MaterialDialog dialog = new MaterialDialog.Builder(context)
									.customView(R.layout.item_dictitem_pop_up_dialog, wrapInScrollView)
									.build();
							ImageView newsstandCover = (ImageView) dialog.getCustomView().findViewById(R.id.tag_newsstand_cover);
							Picasso.with(context).load(magazine.getNewsstandPngUri()).fit().centerInside().placeholder(R.drawable.generic)
									.into(newsstandCover);
							setupSampleButton(context, magazine,
									(Button) dialog.getCustomView().findViewById(R.id.tag_sample));
							setupDownloadButton(context, magazine,
									(Button) dialog.getCustomView().findViewById(R.id.tag_download));
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
							displayable.onThumbnailClick(context);
						}
					});
				}
			}

			if (downloadButton != null) {
				setupDownloadButton(context, magazine, downloadButton);
			}

			if (sampleButton != null) {
				setupSampleButton(context, magazine, sampleButton);
			}

			if (adLayout != null) {
				if (adView == null) {
					String string = context.getString(R.string.dfp_prefix);
					if (TextUtils.isEmpty(string)) {

					} else {
						adView = new PublisherAdView(context);
						adView.setAdUnitId(string + plistName);
						int width = (int) CommonHelper.convertPixelsToDp(context.getResources().getDimension(R.dimen
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

////                    // FIXME Precache all the prices when plist is parsed - parse it while
////                    // splash screen is shown
////                    // FIXME Change library to cache all at once

			if (unitPrice != null) {
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
								//FIXME need to make sure it's still the same textview
								if (magazine.getItemUrl().equals(view.getTag())) {
									unitPrice.setText(skuDetails.priceText);
								}
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								unitPrice.setText("Error");
							}
						});

//                        buy.setText(magazinePrice != null ? magazinePrice : context
//                                .getResources()
//                                .getString(R.string.download));
			}

			if (monthlySubscriptionPrice != null) {
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
									monthlySubscriptionPrice.setText(InAppBillingUtils
											.getFormattedPriceForButton(skuDetails.title, skuDetails.priceText));
								}
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								monthlySubscriptionPrice.setText("Error");
							}
						});

//                        buy.setText(magazinePrice != null ? magazinePrice : context
//                                .getResources()
//                                .getString(R.string.download));
			}

			if (yearlySubscriptionPrice != null) {
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
									yearlySubscriptionPrice.setText(InAppBillingUtils
											.getFormattedPriceForButton(skuDetails.title, skuDetails.priceText));
								}
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable throwable) {
								yearlySubscriptionPrice.setText("Error");
							}
						});

//                        buy.setText(magazinePrice != null ? magazinePrice : context
//                                .getResources()
//                                .getString(R.string.download));
			}

			if (loginButton != null) {
				loginButton.setText(R.string.deja_abonne);
				loginButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

					}
				});
			}
		}

		private void setupDownloadButton(final Context context, final MagazineItem magazine, final Button
				downloadButton) {
			if (magazine.isDownloaded()) {
				downloadButton.setText(R.string.read);
			} else {
				downloadButton.setText(R.string.download);
			}
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
					}
				}
			});
			if (downloadButton instanceof EventBusButton) {
				((EventBusButton) downloadButton).setEventTag(magazine.getItemUrl());
				((EventBusButton) downloadButton).setOnEventListener(new EventBusButton
						.OnEventListener() {

					@Override
					public void onEventListener() {
						if (magazine.getDownloadStatus() == DownloadStatusCode.QUEUED) {
							downloadButton.setText(context.getString(R.string.queued));
						} else if (magazine.getDownloadStatus() == DownloadStatusCode.FAILED) {
							downloadButton.setText(context.getString(R.string.download_failed));
						} else if (magazine.getDownloadStatus() == DownloadStatusCode.DOWNLOADED) {
							downloadButton.setText(context.getString(R.string.read));
						} else {
							downloadButton.setText(String.valueOf(magazine.getDownloadStatus() +
									"%"));
						}
					}
				});

			}

		}

		private void setupSampleButton(final Context context, final MagazineItem magazine, final Button
				sampleButton) {
			if (magazine.isSampleDownloaded()) {
				sampleButton.setText(R.string.read_sample);
			} else {
				sampleButton.setText(R.string.sample);
			}
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
			if (sampleButton instanceof EventBusButton) {
				((EventBusButton) sampleButton).setEventTag(magazine.getSamplePdfUrl());
				((EventBusButton) sampleButton).setOnEventListener(new EventBusButton
						.OnEventListener() {

					@Override
					public void onEventListener() {
						if (magazine.getDownloadStatus() == DownloadStatusCode.QUEUED) {
							sampleButton.setText(context.getString(R.string.queued));
						} else if (magazine.getDownloadStatus() == DownloadStatusCode.FAILED) {
							sampleButton.setText(context.getString(R.string.download_failed));
						} else if (magazine.getDownloadStatus() == DownloadStatusCode.DOWNLOADED) {
							sampleButton.setText(context.getString(R.string.read));
						} else {
							sampleButton.setText(String.valueOf(magazine.getDownloadStatus() +
									"%"));
						}
					}
				});

			}

		}
	}

//	@Override
//	public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
//		super.onViewAttachedToWindow(holder);
//		Log.d("hsdiufh", "holder attached");
//		if (((DefaultViewHolder) holder).sampleButton != null && ((DefaultViewHolder) holder)
//				.sampleButton instanceof EventBusButton) {
//			EventBus.getDefault().register(((DefaultViewHolder) holder).sampleButton);
//			Log.d("hsdiufh", "button registered");
//		}
//	}
//
//	@Override
//	public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
//		super.onViewDetachedFromWindow(holder);
//		Log.d("hsdiufh", "holder detached");
//		if (((DefaultViewHolder) holder).sampleButton != null && ((DefaultViewHolder) holder)
//				.sampleButton instanceof EventBusButton) {
//			EventBus.getDefault().unregister(((DefaultViewHolder) holder).sampleButton);
//			Log.d("hsdiufh", "button unregistered");
//		}
//	}
}

//package com.librelio.adapter;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.librelio.LibrelioApplication;
//import com.librelio.event.ReloadPlistEvent;
//import com.librelio.model.DownloadStatusCode;
//import com.librelio.model.dictitem.DictItem;
//import com.librelio.model.dictitem.DownloadableDictItem;
//import com.librelio.model.dictitem.MagazineItem;
//import com.librelio.model.dictitem.ProductsItem;
//import com.librelio.model.interfaces.DisplayableAsGridItem;
//import com.librelio.service.MagazineDownloadService;
//import com.librelio.storage.DownloadsManager;
//import com.niveales.wind.R;
//import com.squareup.picasso.Picasso;
//
//import java.util.ArrayList;
//
//import de.greenrobot.event.EventBus;
//
//public class DictItemAdapter extends BaseAdapter {
//
//	private Context context;
//	private ArrayList<DictItem> dictItems;
//	private Picasso picasso;
//
//	public DictItemAdapter(ArrayList<DictItem> dictItems, Context context) {
//		this.context = context;
//		this.dictItems = dictItems;
//	}
//
//	@Override
//	public int getCount() {
//		return dictItems.size();
//	}
//
//	@Override
//	public Object getItem(int position) {
//		return position;
//	}
//
//	@Override
//	public long getItemId(int position) {
//		return position;
//	}
//
//	static class DictItemHolder {
//		public TextView title;
//		public TextView subtitle;
//		public ImageView image;
//		public LinearLayout progressLayout;
//		public TextView info;
//		public ProgressBar progressBar;
//		public Button readButton;
//		public Button downloadButton;
//		public Button deleteButton;
//		public Button sampleButton;
//		public Button cancelButton;
//        public ImageButton overflowButton;
//		public int position;
//	}
//
//	@Override
//	public View getView(final int position, View convertView, ViewGroup parent) {
//
//		DictItemHolder holder = new DictItemHolder();
//		if (convertView == null) {
//			convertView = LayoutInflater.from(context).inflate(
//					R.layout.item_dictitem_grid, parent, false);
//			holder.title = (TextView) convertView.findViewById(R.id.item_title);
//			holder.subtitle = (TextView) convertView
//					.findViewById(R.id.item_subtitle);
//			holder.image = (ImageView) convertView
//					.findViewById(R.id.item_thumbnail);
//			holder.progressLayout = (LinearLayout) convertView
//					.findViewById(R.id.item_progress_layout);
//			holder.info = (TextView) convertView.findViewById(R.id.item_info);
//			holder.progressBar = (ProgressBar) convertView
//					.findViewById(R.id.progress_bar);
//			holder.readButton = (Button) convertView
//					.findViewById(R.id.button_read);
//			holder.downloadButton = (Button) convertView
//					.findViewById(R.id.button_download);
//			holder.deleteButton = (Button) convertView
//					.findViewById(R.id.button_delete);
//			holder.sampleButton = (Button) convertView
//					.findViewById(R.id.button_sample);
//			holder.cancelButton = (Button) convertView
//					.findViewById(R.id.button_cancel);
//            holder.overflowButton = (ImageButton) convertView
//                    .findViewById(R.id.button_overflow);
//			convertView.setTag(holder);
//		} else {
//			holder = (DictItemHolder) convertView.getTag();
//		}
//
//		// reset the visibilities
//		holder.title.setText("");
//		holder.subtitle.setText("");
//		holder.progressLayout.setVisibility(View.GONE);
//		holder.progressBar.setVisibility(View.GONE);
//		holder.info.setVisibility(View.GONE);
//		holder.readButton.setVisibility(View.GONE);
//		holder.downloadButton.setVisibility(View.GONE);
//		holder.deleteButton.setVisibility(View.GONE);
//		holder.sampleButton.setVisibility(View.GONE);
//		holder.cancelButton.setVisibility(View.GONE);
//		if (holder.position != position) {
//			holder.position = position;
//			holder.image.setImageDrawable(null);
//		}
//
//		if (dictItems.get(position) instanceof MagazineItem) {
//			final MagazineItem magazine = (MagazineItem) dictItems
//					.get(position);
//
//			int downloadStatus = magazine.getDownloadStatus();
//
//			// If downloading
//			if (downloadStatus >= DownloadStatusCode.QUEUED
//					&& downloadStatus < DownloadStatusCode.DOWNLOADED) {
//				// currently downloading
//
//				holder.progressLayout.setVisibility(View.VISIBLE);
//				holder.progressBar.setVisibility(View.VISIBLE);
//
//				if (downloadStatus == DownloadStatusCode.QUEUED) {
//					holder.info.setVisibility(View.VISIBLE);
//					holder.info.setText(context.getString(R.string.queued));
//				}
//
//				if (downloadStatus > DownloadStatusCode.QUEUED
//						&& downloadStatus < 101) {
//					holder.info.setVisibility(View.VISIBLE);
//					holder.info.setText(context.getResources().getString(
//							R.string.download_in_progress));
//					holder.progressBar.setIndeterminate(false);
//					holder.progressBar.setProgress(downloadStatus);
//				} else {
//					holder.progressBar.setIndeterminate(true);
//				}
//
//				holder.cancelButton.setVisibility(View.VISIBLE);
//				holder.cancelButton.setText(context.getResources().getString(
//						R.string.cancel));
//				holder.cancelButton.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View view) {
//						// FIXME Cancel download
//						DownloadsManager.removeDownload(context,
//                                magazine);
//                        magazine.clearMagazineDir(context);
//					}
//				});
//			} else if (!magazine.isDownloaded()) {
//				holder.downloadButton.setVisibility(View.VISIBLE);
//				if (magazine.isPaid()) {
//					holder.downloadButton.setText(context.getResources()
//							.getString(R.string.download));
//				} else {
//					holder.downloadButton.setText(context.getResources()
//							.getString(R.string.free_Download));
//				}
//				// Sample button
//				if (magazine.isPaid()) {
//					holder.sampleButton.setVisibility(View.VISIBLE);
//					if (magazine.isSampleDownloaded()) {
//						holder.sampleButton.setText(context.getResources()
//								.getString(R.string.read_sample));
//					} else {
//						holder.sampleButton.setText(context.getResources()
//								.getString(R.string.sample));
//					}
//					holder.sampleButton
//							.setOnClickListener(new OnClickListener() {
//								@Override
//								public void onClick(View v) {
//									if (magazine.isSampleDownloaded()) {
//										LibrelioApplication.startPDFActivity(
//												context,
//												magazine.getSamplePdfPath(),
//												magazine.getTitle(), true);
//									} else {
//										MagazineDownloadService
//												.startMagazineDownload(context,
//														magazine, true);
//									}
//								}
//							});
//				}
//			} else if (magazine.isDownloaded()) {
//				// Read case
//				holder.readButton.setVisibility(View.VISIBLE);
//				holder.readButton.setText(context.getResources().getString(
//						R.string.read));
//				holder.readButton.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View v) {
//						LibrelioApplication.startPDFActivity(context,
//								magazine.getItemFilePath(),
//								magazine.getTitle(), true);
//					}
//				});
//			}
//
//			int totalAssetCount = DownloadsManager.getTotalAssetCount(context,
//                    magazine);
//			int downloadedAssetCount = DownloadsManager.getDownloadedAssetCount(
//                    context, magazine);
//			int failedAssetCount = DownloadsManager.getFailedAssetCount(context,
//                    magazine);
//			if ((totalAssetCount > 0)
//					&& (downloadedAssetCount > 0)
//					&& ((downloadedAssetCount + failedAssetCount) < totalAssetCount)) {
//				holder.progressLayout.setVisibility(View.VISIBLE);
//				holder.progressBar.setVisibility(View.VISIBLE);
//				holder.progressBar.setIndeterminate(false);
//				holder.progressBar
//						.setProgress((int) ((downloadedAssetCount * 100.0f) / totalAssetCount));
//				holder.info.setVisibility(View.VISIBLE);
//				holder.info.setText(context.getResources().getString(
//						R.string.downloading_assets)
//						+ "\n" + downloadedAssetCount + "/" + totalAssetCount);
//			}
//
//			// If download failed
//			if (downloadStatus == DownloadStatusCode.FAILED) {
//				holder.info.setText("Download failed");
//				holder.progressLayout.setVisibility(View.VISIBLE);
//				holder.info.setVisibility(View.VISIBLE);
//			}
//
//		} else if (dictItems.get(position) instanceof ProductsItem) {
//			final ProductsItem productsItem = ((ProductsItem) dictItems
//					.get(position));
//
//            int downloadStatus = productsItem.getDownloadStatus();
//
//            // If downloading
//            if (downloadStatus >= DownloadStatusCode.QUEUED
//                    && downloadStatus < DownloadStatusCode.DOWNLOADED) {
//                // currently downloading
//
//                holder.progressLayout.setVisibility(View.VISIBLE);
//                holder.progressBar.setVisibility(View.VISIBLE);
//
//                if (downloadStatus == DownloadStatusCode.QUEUED) {
//                    holder.info.setVisibility(View.VISIBLE);
//                    holder.info.setText(context.getString(R.string.queued));
//                }
//
//                if (downloadStatus > DownloadStatusCode.QUEUED
//                        && downloadStatus < 101) {
//                    holder.info.setVisibility(View.VISIBLE);
//                    holder.info.setText(context.getResources().getString(
//                            R.string.download_in_progress));
//                    holder.progressBar.setIndeterminate(false);
//                    holder.progressBar.setProgress(downloadStatus);
//                } else {
//                    holder.progressBar.setIndeterminate(true);
//                }
//
//                holder.cancelButton.setVisibility(View.VISIBLE);
//                holder.cancelButton.setText(context.getResources().getString(
//                        R.string.cancel));
//                holder.cancelButton.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        // FIXME Cancel download
//                        productsItem.clearMagazineDir(context);
//                        DownloadsManager.removeDownload(context,
//                                productsItem);
//                    }
//                });
//
//            } else if (productsItem.isDownloaded()) {
//				// set as read and delete
//				holder.readButton.setVisibility(View.VISIBLE);
//				holder.readButton.setOnClickListener(new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						productsItem.onReadButtonClicked(context);
//
//					}
//				});
////				holder.deleteButton.setVisibility(View.VISIBLE);
//			} else {
//				// set as download
//				holder.downloadButton.setVisibility(View.VISIBLE);
//                if (productsItem.isPaid()) {
//                    holder.downloadButton.setText(context.getResources()
//                            .getString(R.string.download));
//                } else {
//                    holder.downloadButton.setText(context.getResources()
//                            .getString(R.string.free_Download));
//                }
//			}
//
//            int totalAssetCount = DownloadsManager.getTotalAssetCount(context,
//                    productsItem);
//            int downloadedAssetCount = DownloadsManager.getDownloadedAssetCount(
//                    context, productsItem);
//            int failedAssetCount = DownloadsManager.getFailedAssetCount(context,
//                    productsItem);
//            if ((totalAssetCount > 0)
//                    && (downloadedAssetCount > 0)
//                    && ((downloadedAssetCount + failedAssetCount) < totalAssetCount)) {
//                holder.progressLayout.setVisibility(View.VISIBLE);
//                holder.progressBar.setVisibility(View.VISIBLE);
//                holder.progressBar.setIndeterminate(false);
//                holder.progressBar
//                        .setProgress((int) ((downloadedAssetCount * 100.0f) / totalAssetCount));
//                holder.info.setVisibility(View.VISIBLE);
//                holder.info.setText(context.getResources().getString(
//                        R.string.downloading_assets)
//                        + "\n" + downloadedAssetCount + "/" + totalAssetCount);
//            }
//
//            // If download failed
//            if (downloadStatus == DownloadStatusCode.FAILED) {
//                holder.info.setText("Download failed");
//                holder.progressLayout.setVisibility(View.VISIBLE);
//                holder.info.setVisibility(View.VISIBLE);
//            }
//		}
//
//		if (dictItems.get(position) instanceof DisplayableAsGridItem) {
//			final DisplayableAsGridItem displayable = ((DisplayableAsGridItem) dictItems
//					.get(position));
//			holder.title.setText(displayable.getTitle());
//			holder.subtitle.setText(displayable.getSubtitle());
//			String pngUri = displayable.getPngUri();
//			// Log.d("image", pngUri);
//			Picasso.with(context).load(pngUri).fit().centerInside()
//					.into(holder.image);
//			holder.image.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					displayable.onThumbnailClick(context);
//
//				}
//			});
//		}
//
//		if (dictItems.get(position) instanceof DownloadableDictItem) {
//			holder.downloadButton.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ((DownloadableDictItem) dictItems.get(position))
//                            .onDownloadButtonClick(context);
//                }
//            });
//		}
//
////        if (BuildConfig.DEBUG) {
////            final PopupMenu menu = new PopupMenu(context, holder.overflowButton);
////            menu.getMenu().add(0, 999, 0, "Log asset details");
////            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
////                @Override
////                public boolean onMenuItemClick(MenuItem item) {
////                    switch (item.getItemId()) {
////                        case 999:
////                            DownloadsManager manager = new DownloadsManager(context);
////                            ArrayList<Asset> assetsToDownload = manager
////                                    .getAssetsToDownload();
////                            for (int i = 0; i < assetsToDownload.size(); i++) {
////                                Log.d("DownloadsManager", "assetsToDownload " + i + ": " +
////                                        assetsToDownload.get(i).assetUrl);
////                            }
////                            return true;
////                    }
////                    return false;
////                }
////            });
////
////            holder.overflowButton.setOnClickListener(new OnClickListener() {
////                @Override
////                public void onClick(View v) {
////                    menu.show();
////                }
////            });
////        } else {
////            holder.overflowButton.setVisibility(View.GONE);
////        }
//
//		return convertView;
//	}
//}
