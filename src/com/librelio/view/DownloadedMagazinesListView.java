package com.librelio.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.librelio.LibrelioApplication;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.R;

import java.util.List;

public class DownloadedMagazinesListView extends ListView {
	
	private Context context;
	private MagazinesAdapter magazinesAdapter;
	private List<Magazine> downloads;

	public DownloadedMagazinesListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		setOnItemClickListener();
	}
	
	private void setOnItemClickListener(){
		setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				if (downloads != null){
					Magazine downloadedMagazine = downloads.get(position);
					LibrelioApplication.startPDFActivity(context,
							downloadedMagazine.isSample() ?
							downloadedMagazine.getSamplePdfPath() :
							downloadedMagazine.getItemPath(), downloadedMagazine.getTitle());
				}
			}
		});
	}

	public DownloadedMagazinesListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DownloadedMagazinesListView(Context context) {
		this(context, null, 0);
	}
	
	public void setMagazines(List<Magazine> downloads){
		if (downloads != null){
			this.downloads = downloads; 
			magazinesAdapter = new MagazinesAdapter(context, downloads);
			setAdapter(magazinesAdapter);
		}
	}
}

class MagazinesAdapter extends ArrayAdapter<Magazine> {

	private Context context;
	private List<Magazine> downloads;
	private MagazineManager magazineManager;
	private String samplePostfix;  
	
	public MagazinesAdapter(Context context, List<Magazine> downloads) {
		super(context, R.layout.downloaded_magazines_item, downloads);
		this.context = context;
		this.downloads = downloads;
		
		samplePostfix = new StringBuilder(" (")
							.append(context.getString(R.string.sample))
							.append(")").toString(); 
		
		magazineManager = new MagazineManager(context);
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

		final ViewHolder holder;
		final Magazine downloadedMagazine = this.downloads.get(position);
		
		if ((convertView == null) || (null == convertView.getTag())){

			holder = new ViewHolder();

			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.downloaded_magazines_item, null);

			ImageView image = (ImageView) convertView.findViewById(R.id.downloaded_magasines_item_image);
			holder.image = image;
			
			TextView title = (TextView) convertView.findViewById(R.id.downloaded_magasines_item_title);
			holder.title = title;
			
			TextView editionDate = (TextView) convertView.findViewById(R.id.downloaded_magasines_item_edition_date);
			holder.editionDate = editionDate;
			
			TextView downloadDate = (TextView) convertView.findViewById(R.id.downloaded_magasines_item_download_date);
			holder.downloadDate = downloadDate;
			
			Button deleteButton = (Button) convertView.findViewById(R.id.downloaded_magasines_item_delete_button);
			holder.deleteButton = deleteButton;
			holder.deleteButton.setFocusable(false);
			holder.deleteButton.setFocusableInTouchMode(false);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.title.setText(downloadedMagazine.getTitle() 
				+ (downloadedMagazine.isSample() ? samplePostfix : ""));
		holder.editionDate.setText(downloadedMagazine.getSubtitle());
		holder.downloadDate.setText(downloadedMagazine.getDownloadDate());
		
		holder.deleteButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				downloadedMagazine.clearMagazineDir();
				magazineManager.removeDownloadedMagazine(context, downloadedMagazine);

				getAdapter().remove(downloadedMagazine);
				getAdapter().notifyDataSetChanged();
			}
		});
        holder.position = position;

        // Using an AsyncTask to load the slow images in a background thread
        new AsyncTask<ViewHolder, Void, Bitmap>() {
            private ViewHolder v;

            @Override
            protected Bitmap doInBackground(ViewHolder... params) {
                v = params[0];
                return SystemHelper.decodeSampledBitmapFromFile(downloadedMagazine.getPngPath(),
                                (int) context.getResources().getDimension(R.dimen.preview_image_height),
                                (int) context.getResources().getDimension(R.dimen.preview_image_width));
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                if (v.position == position) {
                    v.image.setImageBitmap(result);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, holder);
		
		return convertView;
	}
	
	private MagazinesAdapter getAdapter(){
		return this;
	}
}

