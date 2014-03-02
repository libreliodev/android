package com.librelio.fragments;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

import org.xml.sax.SAXException;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.librelio.activity.WebViewActivity;
import com.librelio.model.RssFeedItem;
import com.niveales.wind.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class RssFragment extends ListFragment {

	private static final String RSS_FEED_URL = "rss_feed_url";
	public ArrayList<RssItem> rssItems = new ArrayList<RssItem>();
	private RssAdapter adapter;
	
	public static Fragment newInstance(String rssFeedUrl) {
		RssFragment f = new RssFragment();
		Bundle a = new Bundle();
		a.putString(RSS_FEED_URL, rssFeedUrl);
		f.setArguments(a);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new RssAdapter();
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheInMemory(true).cacheOnDisc(true).build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getActivity()).defaultDisplayImageOptions(defaultOptions)
				.build();
		ImageLoader.getInstance().init(config);

		setListAdapter(adapter);

		new RssAsyncTask().execute();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		WebViewActivity.startWithUrl(getActivity(), rssItems.get(position)
				.getLink());
	}

	private class RssAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			URL url;
			try {
				url = new URL(getArguments().getString(RSS_FEED_URL));
				RssFeed feed = RssReader.read(url);

				rssItems = feed.getRssItems();

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			adapter.notifyDataSetChanged();
		}

	}

	private class RssAdapter extends BaseAdapter {

		private final Drawable TRANSPARENT_DRAWABLE = new ColorDrawable(
				Color.TRANSPARENT);

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return rssItems.size();
		}

		@Override
		public RssItem getItem(int position) {
			return rssItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				convertView = View.inflate(getActivity(),
						R.layout.rss_item_row, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.description = (TextView) convertView
						.findViewById(R.id.description);
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			RssItem item = getItem(position);
			holder.title.setText(item.getTitle());
			holder.description.setText(SimpleDateFormat.getDateInstance()
					.format(item.getPubDate())
					+ " - "
					+ stripHtml(item.getDescription()));
			String s = "<img src=\"";
			int ix = item.getDescription().indexOf(s) + s.length();
			String imgSrc = item.getDescription().substring(ix,
					item.getDescription().indexOf("\"", ix + 1));
			holder.image.setImageDrawable(null);
			ImageLoader.getInstance().displayImage(imgSrc, holder.image);
			return convertView;
		}

		private class ViewHolder {
			TextView title;
			TextView description;
			ImageView image;
		}

		public CharSequence stripHtml(String s) {
			return Html.fromHtml(s).toString().replace('\n', (char) 32)
					.replace((char) 160, (char) 32)
					.replace((char) 65532, (char) 32).trim();
		}

	}

}
