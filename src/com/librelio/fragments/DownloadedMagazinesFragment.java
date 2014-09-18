package com.librelio.fragments;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.librelio.event.MagazinesUpdatedEvent;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.storage.MagazineManager;
import com.librelio.view.DownloadedMagazinesListView;
import com.niveales.wind.R;

import de.greenrobot.event.EventBus;

public class DownloadedMagazinesFragment extends ListFragment {

	private MagazineManager magazineManager;
	private DownloadedMagazinesListView listView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_downloaded_magazines, container, false);
		listView = (DownloadedMagazinesListView) view.findViewById(android.R.id.list);
		
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);


		magazineManager = new MagazineManager(getActivity());

		listMagazines();
	}

	private void listMagazines() {
		List<MagazineItem> downloads = magazineManager.getDownloadedMagazines();
		((DownloadedMagazinesListView) getListView()).setMagazines(getActivity(), downloads);
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
	
	public void onEvent(MagazinesUpdatedEvent event) {
		listMagazines();
	}
}
