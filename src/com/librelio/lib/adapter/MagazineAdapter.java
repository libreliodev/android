package com.librelio.lib.adapter;

import java.util.ArrayList;

import com.librelio.lib.model.MagazineModel;
import com.niveales.wind.R;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MagazineAdapter extends BaseAdapter{
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View res;
		
		if(convertView == null){
			res = LayoutInflater.from(context).inflate(R.layout.magazine_list_item, null);
		} else {
			res = convertView;
		}
		TextView title = (TextView)res.findViewById(R.id.item_title);
		TextView subtitle = (TextView)res.findViewById(R.id.item_subtitle);
		ImageView thumbnail = (ImageView)res.findViewById(R.id.item_thumbnail);
		
		title.setText(magazine.get(position).getTitle());
		subtitle.setText(magazine.get(position).getSubtitle());
		
		String imagePath = magazine.get(position).getPngName();
		thumbnail.setImageBitmap(BitmapFactory.decodeFile(imagePath));
		return res;
	}

}
