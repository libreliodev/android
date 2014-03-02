package com.librelio.fragments;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.view.DownloadedMagazinesListView;
import com.niveales.wind.R;

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

		List<Magazine> downloads = magazineManager.getDownloadedMagazines(false);
		((DownloadedMagazinesListView) getListView()).setMagazines(downloads);
	}
}
