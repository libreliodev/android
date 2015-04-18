package com.librelio.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.librelio.model.dictitem.DownloadableDictItem;
import com.librelio.model.dictitem.MagazineItem;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class DownloadedMagazinesAdapter extends ArrayAdapter<DownloadableDictItem> {

	private Context context;
	private List<DownloadableDictItem> downloads;
	private String samplePostfix;

	public DownloadedMagazinesAdapter(Context context) {
		super(context, R.layout.row_downloaded_magazines, 0);
		this.context = context;
		downloads = new ArrayList<>();

		samplePostfix = new StringBuilder(" (")
				.append(context.getString(R.string.sample)).append(")")
				.toString();
	}

	public void setDownloads(Activity activity, final List<DownloadableDictItem> newDownloads) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				downloads.clear();
				downloads.addAll(newDownloads);
				notifyDataSetChanged();
			}
		});
	}

	@Override
	public int getCount() {
		return downloads.size();
	}

	@Override
	public DownloadableDictItem getItem(int position) {
		return downloads.get(position);
	}

	static class ViewHolder {
		public ImageView image;
		public TextView title;
		public TextView editionDate;
		public TextView downloadDate;
		public Button deleteButton;
		public int position = -1;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		final DownloadedMagazinesAdapter.ViewHolder holder;
		if (position < this.downloads.size()) {
			final DownloadableDictItem downloadedItem = this.downloads.get(position);

			if ((convertView == null) || (null == convertView.getTag())) {

				holder = new DownloadedMagazinesAdapter.ViewHolder();

				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(
						R.layout.row_downloaded_magazines, null);

				ImageView image = (ImageView) convertView
						.findViewById(R.id.downloaded_magazines_item_image);
				holder.image = image;

				TextView title = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_title);
				holder.title = title;

				TextView editionDate = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_edition_date);
				holder.editionDate = editionDate;

				TextView downloadDate = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_download_date);
				holder.downloadDate = downloadDate;

				Button deleteButton = (Button) convertView
						.findViewById(R.id.downloaded_magazines_item_delete_button);
				holder.deleteButton = deleteButton;
				holder.deleteButton.setFocusable(false);
				holder.deleteButton.setFocusableInTouchMode(false);

				convertView.setTag(holder);
			} else {
				holder = (DownloadedMagazinesAdapter.ViewHolder) convertView.getTag();
			}

            if (downloadedItem instanceof MagazineItem) {
                holder.title.setText(downloadedItem.getTitle()
                        + (((MagazineItem)downloadedItem).isSample() ? samplePostfix : ""));
            } else {
                holder.title.setText(downloadedItem.getTitle());
            }
			holder.editionDate.setText(downloadedItem.getSubtitle());
			holder.downloadDate.setText(getContext().getString(R.string.downloaded_title) +
                    downloadedItem.getDownloadDate());

			holder.deleteButton.setText(getContext().getString(R.string.delete));
			holder.deleteButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    holder.deleteButton.setText("...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            downloadedItem.deleteItem();
                        }
                    }).start();
                }
            });
			holder.position = position;

			Picasso.with(context).load(downloadedItem.getPngUri()).fit()
					.centerInside().into(holder.image);
		} else {
			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.row_downloaded_magazines,
					null);
		}

		return convertView;
	}
}
