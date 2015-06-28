package com.librelio.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.librelio.LibrelioApplication;
import com.librelio.adapter.DictItemAdapter;
import com.librelio.event.ReloadPlistEvent;
import com.librelio.event.ShowProgressBarEvent;
import com.librelio.model.dictitem.DictItem;
import com.librelio.service.AssetDownloadService;
import com.librelio.utils.PlistDownloader;
import com.librelio.utils.PlistUtils;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class PlistGridFragment extends Fragment {

    private RecyclerView grid;
    private ArrayList<DictItem> dictItems;
    private DictItemAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String plistName;

    private static final String PLIST_NAME = "plist_name";

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

        grid = (RecyclerView) view.findViewById(R.id.issue_list_two_way_view);
        grid.setHasFixedSize(true);
        final GridLayoutManager manager = (GridLayoutManager) grid.getLayoutManager();
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return getResources().getBoolean(R.bool.enable_list_header) ?
                            manager.getSpanCount() : 1;
                } else {
                    return 1;
                }
            }
        });

        grid.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                final Picasso picasso = Picasso.with(getActivity());
                if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView
                        .SCROLL_STATE_DRAGGING) {
                    picasso.resumeTag(getActivity());
                } else {
                    picasso.pauseTag(getActivity());
                }
            }
        });

        dictItems = new ArrayList<>();

        adapter = new DictItemAdapter(getActivity(), dictItems, plistName);
        grid.setAdapter(adapter);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadPlist(true);
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        plistName = getArguments().getString(PLIST_NAME);
        setHasOptionsMenu(true);
        parsePlist();
    }

    @DebugLog
    public void onEventMainThread(ReloadPlistEvent event) {
        if (plistName.equals(event.getPlistName())) {
            parsePlist();
        }
    }

    public void onEventMainThread(ShowProgressBarEvent event) {
        if (plistName.equals(event.getPlistName())) {
//        updateInventory();
            swipeRefreshLayout.setRefreshing(event.isShowProgress());
        }
    }

    @DebugLog
    private void parsePlist() {
        Observable<ArrayList<DictItem>> plistParserObservable = Observable.create(new Observable
                .OnSubscribe<ArrayList<DictItem>>() {
            @Override
            public void call(Subscriber<? super ArrayList<DictItem>> subscriber) {
                ArrayList<DictItem> newDictItems = PlistUtils.parsePlist(getActivity(),
                        plistName);
                subscriber.onNext(newDictItems);
                subscriber.onCompleted();
            }
        });

        AppObservable.bindFragment(this, plistParserObservable)
                .subscribeOn(Schedulers.computation())
                .subscribe(new Action1<ArrayList<DictItem>>() {
                    @Override
                    public void call(ArrayList<DictItem> newDictItems) {
                        if (dictItems != null && grid != null) {
                            // FIXME Need to actually make sure the list of dictItems has changed
                            // rather than just check the size doesn't match
//                            if (dictItems.size() != newDictItems.size()) {
                            dictItems.clear();
                            dictItems.addAll(newDictItems);
//                            grid.invalidate();
                            grid.getAdapter().notifyDataSetChanged();
//                            }
                            // FIXME Shouldn't do this ever 5 seconds
                            // UPDATE : Don't need to!
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
        reloadPlist(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void reloadPlist(boolean force) {
        // force a redownload of the plist
        PlistDownloader.updateFromServer(getActivity(), plistName, force, false);
        // Also try downloading any failed assets
        AssetDownloadService.startAssetDownloadService(getActivity());
    }
}
