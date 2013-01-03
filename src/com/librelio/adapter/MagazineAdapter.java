package com.librelio.adapter;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.librelio.LibrelioApplication;
import com.librelio.activity.BillingActivity;
import com.librelio.activity.DownloadActivity;
import com.librelio.activity.StartupActivity;
import com.librelio.model.MagazineModel;
import com.niveales.wind.R;

public class MagazineAdapter extends BaseAdapter{
	private static final String TAG = "MagazineAdapter";
	private Context context;
	private ArrayList<MagazineModel> magazine;

	
	public MagazineAdapter(ArrayList<MagazineModel> magazine,Context context){
		this.context = context;
		this.magazine = magazine;
	}
	
	@Override
	public int getCount() {
		return magazine.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	static class MagazineItemHolder{
		public TextView title;
		public TextView subtitle;
		public ImageView thumbnail;
		public Button downloadOrReadButton;
		public Button sampleOrDeleteButton;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final MagazineModel currentMagazine = magazine.get(position);
		MagazineItemHolder holder = new MagazineItemHolder();
		
		if(convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.magazine_list_item, null);
			holder.title = (TextView)convertView.findViewById(R.id.item_title);;
			holder.subtitle = (TextView)convertView.findViewById(R.id.item_subtitle);;
			holder.thumbnail = (ImageView)convertView.findViewById(R.id.item_thumbnail);
			/**
			 * downloadOrReadButton - this button can be "Download button" or "Read button"
			 */
			holder.downloadOrReadButton = (Button)convertView.findViewById(R.id.item_button_one);
			/**
			 * sampleOrDeleteButton - this button can be "Delete button" or "Sample button"
			 */
			holder.sampleOrDeleteButton = (Button)convertView.findViewById(R.id.item_button_two);
			convertView.setTag(holder);
		} else {
			holder = (MagazineItemHolder)convertView.getTag();
		}
		holder.title.setText(currentMagazine.getTitle());
		holder.subtitle.setText(currentMagazine.getSubtitle());
		
		String imagePath = currentMagazine.getPngPath();
		holder.thumbnail.setImageBitmap(BitmapFactory.decodeFile(imagePath));
		
		/**
		 * TODO delete after testing
		 */
		if(currentMagazine.getFileName().equals(StartupActivity.TEST_FILE_NAME)){
			holder.sampleOrDeleteButton.setVisibility(View.INVISIBLE);
			holder.downloadOrReadButton.setText(context.getResources().getString(
					R.string.read));
			holder.downloadOrReadButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(new File(currentMagazine.getPdfPath()).exists()){
						LibrelioApplication.startPDFActivity(context,
								currentMagazine.getPdfPath());
					} else {
						Toast.makeText(context, "No test pdf, check assets dir", Toast.LENGTH_SHORT).show();
					}
				}
			});
			return convertView;
		}
		
		if (currentMagazine.isDownloaded()) {
			// Read case
			holder.downloadOrReadButton.setText(context.getResources().getString(
					R.string.read));
			holder.downloadOrReadButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LibrelioApplication.startPDFActivity(context,
							currentMagazine.getPdfPath());
				}
			});
			//
			/*PDFParser linkGetter = new PDFParser(currentMagazine.getPdfPath());
			SparseArray<LinkInfo[]> linkBuf = linkGetter.getLinkInfo();
			if(linkBuf==null){
				Log.d(TAG,"There is no links");
			} else {
				for(int i=0;i<linkBuf.size();i++){
					Log.d(TAG,"--- i = "+i);
					if(linkBuf.get(i)!=null){
						for(int j=0;j<linkBuf.get(i).length;j++){
							String link = linkBuf.get(i)[j].uri;
							Log.d(TAG,"link[" + j + "] = "+link);
							String local = "http://localhost";
							if(link.startsWith(local)){
								int startIdx = local.length()+1;
								int finIdx = link.length();
								if(link.contains("?")){
									finIdx = link.indexOf("?");
								}
								String assetsFile = link.substring(startIdx, finIdx);
								Log.d(TAG,"   link: "+MagazineModel.getAssetsBaseURL(currentMagazine.getPdfPath())+assetsFile);
								Log.d(TAG,"   file: "+assetsFile);
							}
						}
					}
				}
			}*/
			//
		} else {
			// download case
			holder.downloadOrReadButton.setText(context.getResources().getString(
					R.string.download));
			holder.downloadOrReadButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(currentMagazine.isPaid()){
						Intent intent = new Intent(context,BillingActivity.class);
						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
						context.startActivity(intent);
					} else {
						Intent intent = new Intent(context,DownloadActivity.class);
						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
						intent.putExtra(DownloadActivity.IS_SAMPLE_KEY,false);
						context.startActivity(intent);
					}
				}
			});
		}
		//
		holder.sampleOrDeleteButton.setVisibility(View.VISIBLE);
		if (!currentMagazine.isPaid() && !currentMagazine.isDownloaded()) {
			holder.sampleOrDeleteButton.setVisibility(View.INVISIBLE);
		} else if (currentMagazine.isDownloaded()) {
				// delete case
			holder.sampleOrDeleteButton.setText(context.getResources().getString(
						R.string.delete));
			holder.sampleOrDeleteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				currentMagazine.delete();
			}
			});
		} else {
			// Sample case
			holder.sampleOrDeleteButton.setText(context.getResources().getString(
					R.string.sample));
			holder.sampleOrDeleteButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					File sample = new File(currentMagazine.getSamplePath());
					Log.d(TAG, "test: " + sample.exists() + " "
							+ currentMagazine.isSampleDownloaded());
					if (currentMagazine.isSampleDownloaded()) {
						LibrelioApplication.startPDFActivity(context,
								currentMagazine.getSamplePath());
					} else {
						Intent intent = new Intent(context,
								DownloadActivity.class);
						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
						intent.putExtra(DownloadActivity.IS_SAMPLE_KEY,true);
						context.startActivity(intent);
					}
				}
			});
		}

		return convertView;
	}

}
