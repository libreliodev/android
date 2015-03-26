package com.librelio.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.librelio.event.NewPlistDownloadedEvent;
import com.librelio.event.ReloadPlistEvent;
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

        grid = (RecyclerView) view.findViewById(R.id.issue_list_two_way_view);
        grid.setHasFixedSize(true);
//        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(),
//                getResources().getInteger(R.integer.plist_grid_num_columns));
//        grid.setLayoutManager(layoutManager);
        final GridLayoutManager manager = (GridLayoutManager) grid.getLayoutManager();
//        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
//            @Override
//            public int getSpanSize(int position) {
//                return adapter.isHeader(position) ? manager.getSpanCount() : 1;
//            }
//        });
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return manager.getSpanCount();
                } else {
                    return 1;
                }
//                switch(adapter.getItemViewType(position)){
//                    case MyAdapter.TYPE_HEADER:
//                        return 2;
//                    case MyAdapter.TYPE_ITEM:
//                        return 1;
//                    default:
//                        return -1;
//                }
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

        adapter = new DictItemAdapter(getActivity(), dictItems);
        grid.setAdapter(adapter);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.LARGE);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadPlist();
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        plistName = getArguments().getString(PLIST_NAME);
        setHasOptionsMenu(true);
    }

    @DebugLog
    public void onEvent(ReloadPlistEvent event) {
        startDisplayDictItemsTaskWithDelay(0);
    }

    public void onEvent(NewPlistDownloadedEvent event) {
//        updateInventory();
        swipeRefreshLayout.setRefreshing(false);
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
                            // FIXME Shouldn't do this ever 5 seconds
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

    private void reloadPlist() {
        // force a redownload of the plist
        PlistDownloader.updateFromServer(getActivity(), plistName, true);
        // Also try downloading any failed assets
        AssetDownloadService.startAssetDownloadService(getActivity());
    }

    private void startDisplayDictItemsTaskWithDelay(int delay) {
        handler.removeCallbacks(displayDictItemsTask);
        handler.postDelayed(displayDictItemsTask, delay);
    }
}
