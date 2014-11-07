package com.librelio.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.librelio.LibrelioApplication;
import com.librelio.event.LoadPlistEvent;
import com.librelio.model.Asset;
import com.librelio.model.DownloadStatusCode;
import com.librelio.model.dictitem.DictItem;
import com.librelio.model.dictitem.DownloadableDictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.model.dictitem.ProductsItem;
import com.librelio.model.interfaces.DisplayableAsGridItem;
import com.librelio.service.MagazineDownloadService;
import com.librelio.storage.DownloadsManager;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class DictItemAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<DictItem> dictItems;
	private Picasso picasso;

	public DictItemAdapter(ArrayList<DictItem> dictItems, Context context) {
		this.context = context;
		this.dictItems = dictItems;
	}

	@Override
	public int getCount() {
		return dictItems.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	static class DictItemHolder {
		public TextView title;
		public TextView subtitle;
		public ImageView thumbnail;
		public LinearLayout progressLayout;
		public TextView info;
		public ProgressBar progressBar;
		public Button readButton;
		public Button downloadButton;
		public Button deleteButton;
		public Button sampleButton;
		public Button cancelButton;
        public ImageButton overflowButton;
		public int position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		DictItemHolder holder = new DictItemHolder();
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					R.layout.magazine_list_item, parent, false);
			holder.title = (TextView) convertView.findViewById(R.id.item_title);
			holder.subtitle = (TextView) convertView
					.findViewById(R.id.item_subtitle);
			holder.thumbnail = (ImageView) convertView
					.findViewById(R.id.item_thumbnail);
			holder.progressLayout = (LinearLayout) convertView
					.findViewById(R.id.item_progress_layout);
			holder.info = (TextView) convertView.findViewById(R.id.item_info);
			holder.progressBar = (ProgressBar) convertView
					.findViewById(R.id.progress_bar);
			holder.readButton = (Button) convertView
					.findViewById(R.id.button_read);
			holder.downloadButton = (Button) convertView
					.findViewById(R.id.button_download);
			holder.deleteButton = (Button) convertView
					.findViewById(R.id.button_delete);
			holder.sampleButton = (Button) convertView
					.findViewById(R.id.button_sample);
			holder.cancelButton = (Button) convertView
					.findViewById(R.id.button_cancel);
            holder.overflowButton = (ImageButton) convertView
                    .findViewById(R.id.button_overflow);
			convertView.setTag(holder);
		} else {
			holder = (DictItemHolder) convertView.getTag();
		}

		// reset the visibilities
		holder.title.setText("");
		holder.subtitle.setText("");
		holder.progressLayout.setVisibility(View.GONE);
		holder.progressBar.setVisibility(View.GONE);
		holder.info.setVisibility(View.GONE);
		holder.readButton.setVisibility(View.GONE);
		holder.downloadButton.setVisibility(View.GONE);
		holder.deleteButton.setVisibility(View.GONE);
		holder.sampleButton.setVisibility(View.GONE);
		holder.cancelButton.setVisibility(View.GONE);
		if (holder.position != position) {
			holder.position = position;
			holder.thumbnail.setImageDrawable(null);
		}

		if (dictItems.get(position) instanceof MagazineItem) {
			final MagazineItem magazine = (MagazineItem) dictItems
					.get(position);

			int downloadStatus = magazine.getDownloadStatus();

			// If downloading
			if (downloadStatus >= DownloadStatusCode.QUEUED
					&& downloadStatus < DownloadStatusCode.DOWNLOADED) {
				// currently downloading

				holder.progressLayout.setVisibility(View.VISIBLE);
				holder.progressBar.setVisibility(View.VISIBLE);

				if (downloadStatus == DownloadStatusCode.QUEUED) {
					holder.info.setVisibility(View.VISIBLE);
					holder.info.setText(context.getString(R.string.queued));
				}

				if (downloadStatus > DownloadStatusCode.QUEUED
						&& downloadStatus < 101) {
					holder.info.setVisibility(View.VISIBLE);
					holder.info.setText(context.getResources().getString(
							R.string.download_in_progress));
					holder.progressBar.setIndeterminate(false);
					holder.progressBar.setProgress(downloadStatus);
				} else {
					holder.progressBar.setIndeterminate(true);
				}

				holder.cancelButton.setVisibility(View.VISIBLE);
				holder.cancelButton.setText(context.getResources().getString(
						R.string.cancel));
				holder.cancelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						// FIXME Cancel download
						magazine.clearMagazineDir(context);
						DownloadsManager.removeDownload(context,
                                magazine);
						EventBus.getDefault().post(new LoadPlistEvent());
					}
				});
			} else if (!magazine.isDownloaded()) {
				holder.downloadButton.setVisibility(View.VISIBLE);
				if (magazine.isPaid()) {
					holder.downloadButton.setText(context.getResources()
							.getString(R.string.download));
				} else {
					holder.downloadButton.setText(context.getResources()
							.getString(R.string.free_Download));
				}
				// Sample button
				if (magazine.isPaid()) {
					holder.sampleButton.setVisibility(View.VISIBLE);
					if (magazine.isSampleDownloaded()) {
						holder.sampleButton.setText(context.getResources()
								.getString(R.string.read_sample));
					} else {
						holder.sampleButton.setText(context.getResources()
								.getString(R.string.sample));
					}
					holder.sampleButton
							.setOnClickListener(new OnClickListener() {
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
			} else if (magazine.isDownloaded()) {
				// Read case
				holder.readButton.setVisibility(View.VISIBLE);
				holder.readButton.setText(context.getResources().getString(
						R.string.read));
				holder.readButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						LibrelioApplication.startPDFActivity(context,
								magazine.getItemFilePath(),
								magazine.getTitle(), true);
					}
				});
			}

			int totalAssetCount = DownloadsManager.getTotalAssetCount(context,
                    magazine);
			int downloadedAssetCount = DownloadsManager.getDownloadedAssetCount(
                    context, magazine);
			int failedAssetCount = DownloadsManager.getFailedAssetCount(context,
                    magazine);
			if ((totalAssetCount > 0)
					&& (downloadedAssetCount > 0)
					&& ((downloadedAssetCount + failedAssetCount) < totalAssetCount)) {
				holder.progressLayout.setVisibility(View.VISIBLE);
				holder.progressBar.setVisibility(View.VISIBLE);
				holder.progressBar.setIndeterminate(false);
				holder.progressBar
						.setProgress((int) ((downloadedAssetCount * 100.0f) / totalAssetCount));
				holder.info.setVisibility(View.VISIBLE);
				holder.info.setText(context.getResources().getString(
						R.string.downloading_assets)
						+ "\n" + downloadedAssetCount + "/" + totalAssetCount);
			}

			// If download failed
			if (downloadStatus == DownloadStatusCode.FAILED) {
				holder.info.setText("Download failed");
				holder.progressLayout.setVisibility(View.VISIBLE);
				holder.info.setVisibility(View.VISIBLE);
			}

		} else if (dictItems.get(position) instanceof ProductsItem) {
			final ProductsItem productsItem = ((ProductsItem) dictItems
					.get(position));

            int downloadStatus = productsItem.getDownloadStatus();

            // If downloading
            if (downloadStatus >= DownloadStatusCode.QUEUED
                    && downloadStatus < DownloadStatusCode.DOWNLOADED) {
                // currently downloading

                holder.progressLayout.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);

                if (downloadStatus == DownloadStatusCode.QUEUED) {
                    holder.info.setVisibility(View.VISIBLE);
                    holder.info.setText(context.getString(R.string.queued));
                }

                if (downloadStatus > DownloadStatusCode.QUEUED
                        && downloadStatus < 101) {
                    holder.info.setVisibility(View.VISIBLE);
                    holder.info.setText(context.getResources().getString(
                            R.string.download_in_progress));
                    holder.progressBar.setIndeterminate(false);
                    holder.progressBar.setProgress(downloadStatus);
                } else {
                    holder.progressBar.setIndeterminate(true);
                }

                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setText(context.getResources().getString(
                        R.string.cancel));
                holder.cancelButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // FIXME Cancel download
                        productsItem.clearMagazineDir(context);
                        DownloadsManager.removeDownload(context,
                                productsItem);
                        EventBus.getDefault().post(new LoadPlistEvent());
                    }
                });

            } else if (productsItem.isDownloaded()) {
				// set as read and delete
				holder.readButton.setVisibility(View.VISIBLE);
				holder.readButton.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						productsItem.onReadButtonClicked(context);
						
					}
				});
//				holder.deleteButton.setVisibility(View.VISIBLE);
			} else {
				// set as download
				holder.downloadButton.setVisibility(View.VISIBLE);
                if (productsItem.isPaid()) {
                    holder.downloadButton.setText(context.getResources()
                            .getString(R.string.download));
                } else {
                    holder.downloadButton.setText(context.getResources()
                            .getString(R.string.free_Download));
                }
			}

            int totalAssetCount = DownloadsManager.getTotalAssetCount(context,
                    productsItem);
            int downloadedAssetCount = DownloadsManager.getDownloadedAssetCount(
                    context, productsItem);
            int failedAssetCount = DownloadsManager.getFailedAssetCount(context,
                    productsItem);
            if ((totalAssetCount > 0)
                    && (downloadedAssetCount > 0)
                    && ((downloadedAssetCount + failedAssetCount) < totalAssetCount)) {
                holder.progressLayout.setVisibility(View.VISIBLE);
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setIndeterminate(false);
                holder.progressBar
                        .setProgress((int) ((downloadedAssetCount * 100.0f) / totalAssetCount));
                holder.info.setVisibility(View.VISIBLE);
                holder.info.setText(context.getResources().getString(
                        R.string.downloading_assets)
                        + "\n" + downloadedAssetCount + "/" + totalAssetCount);
            }

            // If download failed
            if (downloadStatus == DownloadStatusCode.FAILED) {
                holder.info.setText("Download failed");
                holder.progressLayout.setVisibility(View.VISIBLE);
                holder.info.setVisibility(View.VISIBLE);
            }
		}

		if (dictItems.get(position) instanceof DisplayableAsGridItem) {
			final DisplayableAsGridItem displayable = ((DisplayableAsGridItem) dictItems
					.get(position));
			holder.title.setText(displayable.getTitle());
			holder.subtitle.setText(displayable.getSubtitle());
			String pngUri = displayable.getPngUri();
			// Log.d("image", pngUri);
			Picasso.with(context).load(pngUri).fit().centerInside()
					.into(holder.thumbnail);
			holder.thumbnail.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					displayable.onThumbnailClick(context);

				}
			});
		}

		if (dictItems.get(position) instanceof DownloadableDictItem) {
			holder.downloadButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((DownloadableDictItem) dictItems.get(position))
                            .onDownloadButtonClick(context);
                }
            });
		}

        if (BuildConfig.DEBUG) {
            final PopupMenu menu = new PopupMenu(context, holder.overflowButton);
            menu.getMenu().add(0, 999, 0, "Log asset details");
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case 999:
                            DownloadsManager manager = new DownloadsManager(context);
                            ArrayList<Asset> assetsToDownload = manager
                                    .getAssetsToDownload();
                            for (int i = 0; i < assetsToDownload.size(); i++) {
                                Log.d("DownloadsManager", "assetsToDownload " + i + ": " +
                                        assetsToDownload.get(i).assetUrl);
                            }
                            return true;
                    }
                    return false;
                }
            });

            holder.overflowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    menu.show();
                }
            });
        } else {
            holder.overflowButton.setVisibility(View.GONE);
        }

		return convertView;
	}
}
