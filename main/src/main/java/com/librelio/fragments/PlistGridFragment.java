package com.librelio.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import com.librelio.event.ReloadPlistEvent;
import com.librelio.model.dictitem.DictItem;
import com.librelio.service.AssetDownloadService;
import com.librelio.utils.PlistDownloader;
import com.librelio.utils.PlistUtils;
import com.niveales.wind.R;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class PlistGridFragment extends Fragment {

    private GridView grid;
    private ArrayList<DictItem> dictItems;
    private DictItemAdapter adapter;

    private String plistName;

    private static final String PLIST_NAME = "plist_name";

    private Handler handler = new Handler();

    private Runnable displayDictItemsTask = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parsePlist();
                        adapter.notifyDataSetChanged();
                        // Repeat every 5 seconds
                        startDisplayDictItemsTaskWithDelay(5000);
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

        dictItems = new ArrayList<>();

        adapter = new DictItemAdapter(dictItems, getActivity());
        grid.setAdapter(adapter);

        plistName = getArguments().getString(PLIST_NAME);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @DebugLog
    public void onEvent(ReloadPlistEvent event) {
        startDisplayDictItemsTaskWithDelay(0);
    }

    @DebugLog
    private void parsePlist() {
        Observable<ArrayList<DictItem>> plistParserObservable = Observable.create(new Observable
                .OnSubscribe<ArrayList<DictItem>>() {
            @Override
            public void call(Subscriber<? super ArrayList<DictItem>> subscriber) {
                ArrayList<DictItem> dictItems = PlistUtils.parsePlist(getActivity(),
                        plistName);
                subscriber.onNext(dictItems);
                subscriber.onCompleted();
            }
        });

        AppObservable.bindFragment(this, plistParserObservable)
                .subscribeOn(Schedulers.computation())
                .subscribe(new Action1<ArrayList<DictItem>>() {
                    @Override
                    public void call(ArrayList<DictItem> newDictItems) {
                        if (dictItems != null && grid != null) {
                            dictItems.clear();
                            dictItems.addAll(newDictItems);
                            grid.invalidate();
                            grid.invalidateViews();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        Tracker tracker = ((LibrelioApplication) getActivity().getApplication()).getTracker();
        tracker.setScreenName("Library/Magazines");
        tracker.send(new HitBuilders.AppViewBuilder().build());
        EventBus.getDefault().register(this);
        startDisplayDictItemsTaskWithDelay(0);
        PlistDownloader.updateFromServer(getActivity(), plistName, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(displayDictItemsTask);
        EventBus.getDefault().unregister(this);
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
                PlistDownloader.updateFromServer(getActivity(), plistName, true);
                // Also try downloading any failed assets
                AssetDownloadService.startAssetDownloadService(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startDisplayDictItemsTaskWithDelay(int delay) {
        handler.removeCallbacks(displayDictItemsTask);
        handler.postDelayed(displayDictItemsTask, delay);
    }
}
