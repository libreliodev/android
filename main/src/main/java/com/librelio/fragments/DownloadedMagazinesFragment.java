package com.librelio.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.librelio.adapter.DownloadedMagazinesAdapter;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.model.dictitem.DownloadableDictItem;
import com.librelio.storage.DownloadsManager;
import com.niveales.wind.R;

import java.util.List;

import de.greenrobot.event.EventBus;

public class DownloadedMagazinesFragment extends ListFragment {

	private DownloadsManager downloadsManager;
	private ListView listView;
	private DownloadedMagazinesAdapter magazinesAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_downloaded_magazines, container, false);
		listView = (ListView) view.findViewById(android.R.id.list);

		magazinesAdapter = new DownloadedMagazinesAdapter(getActivity());
		listView.setAdapter(magazinesAdapter);

		//		setOnItemClickListener(new OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view,
//					int position, long id) {
//
//				MagazineItem downloadedMagazine = magazinesAdapter
//						.getItem(position);
//				LibrelioApplication.startPDFActivity(
//						context,
//						downloadedMagazine.isSample() ? downloadedMagazine
//								.getSamplePdfPath() : downloadedMagazine
//								.getItemFilePath(), downloadedMagazine
//								.getTitle(), true);
//			}
//		});
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


		downloadsManager = new DownloadsManager(getActivity());

		listMagazines();
	}

	private void listMagazines() {
		List<DownloadableDictItem> downloads = downloadsManager.getDownloadedMagazines();
		magazinesAdapter.setDownloads(getActivity(), downloads);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		listMagazines();
		EventBus.getDefault().register(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		EventBus.getDefault().unregister(this);
	}
	
	public void onEvent(ReloadPlistEvent event) {
		listMagazines();
	}
}
