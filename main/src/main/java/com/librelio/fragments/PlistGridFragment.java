package com.librelio.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.librelio.LibrelioApplication;
import com.librelio.adapter.DictItemAdapter;
import com.librelio.base.BaseActivity;
import com.librelio.event.InvalidateGridViewEvent;
import com.librelio.event.LoadPlistEvent;
import com.librelio.event.PlistUpdatedEvent;
import com.librelio.event.UpdateMagazinesEvent;
import com.librelio.loader.PlistParserLoader;
import com.librelio.model.dictitem.DictItem;
import com.librelio.service.AssetDownloadService;
import com.librelio.utils.PlistDownloader;
import com.niveales.wind.R;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class PlistGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<DictItem>> {

	private GridView grid;
	private ArrayList<DictItem> magazines;
	private DictItemAdapter adapter;
	
    private String plistName;
    
    private static final int PLIST_PARSER_LOADER = 0;
	private static final String PLIST_NAME = "plist_name";

    private Handler handler = new Handler();

	private Runnable updateGridTask = new Runnable() {
		@Override
		public void run() {
			if (getActivity() != null) {
				
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						getLoaderManager().restartLoader(PLIST_PARSER_LOADER,
								null, PlistGridFragment.this);
						adapter.notifyDataSetChanged();
						startUpdateGridTask(5000);
					}
				});
			}
		}
	};
    
	public static PlistGridFragment newInstance(String plistName) {
		PlistGridFragment f = new PlistGridFragment();
		Bundle a = new Bundle();
		a.putString(PLIST_NAME, plistName);
		f.setArguments(a);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_plist_grid, container, false);

		grid = (GridView) view.findViewById(R.id.issue_list_grid_view);
		
		magazines = new ArrayList<DictItem>();

		adapter = new DictItemAdapter(magazines, getActivity());
		grid.setAdapter(adapter);
		
		plistName = getArguments().getString(PLIST_NAME);
		
		getLoaderManager().initLoader(PLIST_PARSER_LOADER, null, this);
		
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        setHasOptionsMenu(true);

	}
	
    public void onEventMainThread(UpdateMagazinesEvent event) {
        if (event.getMagazines() != null && event.getPlistName().equals(plistName)) {
            magazines.clear();
            magazines.addAll(event.getMagazines());
        }
        reloadGrid();
    }
    
    public void onEventMainThread(PlistUpdatedEvent event) {
    	getLoaderManager().restartLoader(PLIST_PARSER_LOADER,
				null, PlistGridFragment.this);
    }

    public void onEventMainThread(InvalidateGridViewEvent event) {
            reloadGrid();
    }

    public void onEvent(LoadPlistEvent event) {
        startUpdateGridTask(0);
    }

	private void reloadGrid() {
        grid.invalidate();
        grid.invalidateViews();
	}
	
	@Override
	public void onResume() {
		super.onResume();
        Tracker tracker = ((LibrelioApplication)getActivity().getApplication()).getTracker();
        tracker.setScreenName("Library/Magazines");
        tracker.send(new HitBuilders.AppViewBuilder().build());
		EventBus.getDefault().register(this);
        startUpdateGridTask(0);
        PlistDownloader.doLoad(getActivity(), plistName, false);
    }

    @Override
	public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        handler.removeCallbacks(updateGridTask);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
    	inflater.inflate(R.menu.fragment_magazines, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case R.id.options_menu_reload:
            // force a redownload of the plist
            PlistDownloader.doLoad(getActivity(), plistName, true);
            // Also try downloading any failed assets
        	AssetDownloadService.startAssetDownloadService(getActivity());
			return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public Loader<ArrayList<DictItem>> onCreateLoader(int id, Bundle args) {
        return new PlistParserLoader(getActivity().getApplicationContext(), plistName);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<DictItem>> loader, ArrayList<DictItem> data) {
//        magazines.clear();
//        if (data != null) {
//            magazines.addAll(data);
//        }
        EventBus.getDefault().post(new InvalidateGridViewEvent());
        startUpdateGridTask(2000);
    }

    private void startUpdateGridTask(int delay) {
        handler.removeCallbacks(updateGridTask);
        handler.postDelayed(updateGridTask, delay);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<DictItem>> loader) {
        magazines.clear();
        EventBus.getDefault().post(new InvalidateGridViewEvent());
    }
    
    private BaseActivity getBaseActivity() {
    	return (BaseActivity) getActivity();
    }

}
