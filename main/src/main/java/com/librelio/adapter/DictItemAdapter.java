package com.librelio.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.view.MagazineGridItemView;
import com.niveales.wind.R;

import java.util.ArrayList;

public class DictItemAdapter extends RecyclerView.Adapter {

	private static final int TYPE_HEADER = 2;
	private static final int TYPE_DEFAULT = 1;

	private final Context context;
	private final ArrayList<DictItem> dictItems;
	private String plistName;
	private final boolean enableListHeader;

	public DictItemAdapter(Context context, ArrayList<DictItem> dictItems, String plistName) {
		this.context = context;
		this.dictItems = dictItems;
		this.plistName = plistName;
		enableListHeader = context.getResources().getBoolean(R.bool.enable_list_header);
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_HEADER:
				final MagazineGridItemView header =
						new MagazineGridItemView(context, MagazineGridItemView.HEADER, plistName);
				return new DefaultViewHolder(context, header, plistName);
			case TYPE_DEFAULT:
				final MagazineGridItemView view =
						new MagazineGridItemView(context, MagazineGridItemView.STANDARD, plistName);
				return new DefaultViewHolder(context, view, plistName);
		}
		return null;
	}

	@Override
	public int getItemViewType(int position) {
		if (enableListHeader && position == 0) {
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

	public static class DefaultViewHolder extends RecyclerView.ViewHolder {

		private final Context context;
		private final MagazineGridItemView view;

		public DefaultViewHolder(Context context, MagazineGridItemView view, String plistName) {
			super(view);
			this.context = context;
			this.view = view;
		}

		public void bind(final DictItem dictItem) {

			view.setMagazine((MagazineItem) dictItem);


////                    // FIXME Precache all the prices when plist is parsed - parse it while
////                    // splash screen is shown
////                    // FIXME Change library to cache all at once


		}
	}
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
