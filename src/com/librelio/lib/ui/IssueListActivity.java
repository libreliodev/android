package com.librelio.lib.ui;


import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;


import com.librelio.base.BaseActivity;
import com.librelio.lib.ui.IssueListAdapter.IssueListEventListener;
import com.librelio.lib.utils.cloud.CloudHelper;
import com.librelio.lib.utils.cloud.Issue;
import com.librelio.lib.utils.cloud.CloudHelper.CloudEventListener;
import com.librelio.lib.utils.db.Ocean;
import com.niveales.wind.R;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.Toast;

public class IssueListActivity extends BaseActivity implements IssueListEventListener, CloudEventListener {
	@SuppressWarnings("unused")
	private static final String TAG = IssueListActivity.class.getSimpleName();


	
	
	public CloudHelper cloud;
	public GridView mIssueListGrid;
	public DownloadManager mDownloadManager;
//	public long enqueue;
	public HashMap<Long, Long> mDownloadRequestsHashMap; // <enqueueId, issueId>
	public BroadcastReceiver mDownloadBroadcasReciever;
        
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.issue_list_layout);
        
        
        cloud = new CloudHelper(this, this);
        mDownloadManager = (DownloadManager) this.getSystemService(Activity.DOWNLOAD_SERVICE);
        
        mIssueListGrid = (GridView) findViewById(R.id.issue_list_grid_view);
        
        
        if(cloud.getAllIssueCursor().getCount() > 0 ){
        	setGridViewAdapter(mIssueListGrid);
        
        } else {
        	// Need to download issues for the first time
        	onReloadIssues();
        }
        mDownloadRequestsHashMap = new HashMap<Long, Long>();
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        mDownloadBroadcasReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Query query = new Query();
                    query.setFilterById(downloadId);
                    Cursor c = mDownloadManager.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c
                                .getInt(columnIndex)) {
                        	String uriString = c
                                    .getString(c
                                            .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        	Issue issue = cloud.getIssue(mDownloadRequestsHashMap.get(downloadId));
                        	issue.setState(Issue.STATE_LOADED);
                        	issue.setIssue_path(uriString);
                        	cloud.updateIssue(issue);
                        	mIssueListGrid.invalidateViews();
                        	mDownloadRequestsHashMap.remove(downloadId);
                        }
                    }
                }
            }
        };
        this.registerReceiver(mDownloadBroadcasReciever, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

	public void onReloadIssues(){
		if(this.isOnline()){
			Toast.makeText(this, R.string.searching_, Toast.LENGTH_SHORT).show();
			try {
				cloud.reloadIssues();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}else{
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
		}
		
	}
	
	@Override
	public void onReadClick(long issueId) {
		Issue issue = cloud.getIssue(issueId);
		Uri uri = Uri.parse(issue.getIssue_path());
		Intent intent = new Intent(this,com.artifex.mupdf.MuPDFActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(uri);
		startActivity(intent);
		
	}

	@Override
	public void onDownloadClick(long issueId) {
		try {
			long enqueue = cloud.downloadIssue(issueId);
			this.mDownloadRequestsHashMap.put(enqueue, issueId);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mIssueListGrid.invalidateViews();
	}
	
	@Override
	public void onDestroy(){
		this.unregisterReceiver(mDownloadBroadcasReciever);
		for( Iterator <Long> mDownloadRequestsIterator = mDownloadRequestsHashMap.keySet().iterator();
			mDownloadRequestsIterator.hasNext(); mDownloadManager.remove(mDownloadRequestsIterator.next()));
		cloud.recycle();
		super.onDestroy();
	}
	
	private boolean isOnline() {
	    ConnectivityManager cm =
	            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

	        return cm.getActiveNetworkInfo() != null && 
	           cm.getActiveNetworkInfo().isConnectedOrConnecting();
	    }
	
	public void setGridViewAdapter(GridView g){
		String [] from = {Ocean.ISSUE_COVER_PATH_KEY, Ocean.ISSUE_NAME_KEY, Ocean.ISSUE_STATE_KEY};
        int [] to = {R.id.issue_cover, R.id.issue_title, R.id.issue_button};
        mIssueListGrid = (GridView) findViewById(R.id.issue_list_grid_view);
        Cursor mAllIssuesCursor = cloud.getAllIssueCursor();
        g.setAdapter(new IssueListAdapter(this, R.layout.issue_item_unloaded, mAllIssuesCursor, 
        		from, to, cloud, this));
	}

	@Override
	public void onReloadIssuesFinished() {
		IssueListAdapter a = ((IssueListAdapter)mIssueListGrid.getAdapter());
		if(a == null){
			setGridViewAdapter(mIssueListGrid);
		} else {
			a.notifyDataSetInvalidated();
			this.mIssueListGrid.invalidate();
		}
//		((IssueListAdapter)mIssueListGrid.getAdapter()).notifyDataSetChanged();
//		mIssueListGrid.invalidateViews();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    MenuItem item = menu.getItem(0);
	    item.setIcon(R.drawable.ic_menu_refresh);
	    MenuItemCompat.setShowAsAction(menu.getItem(0), MenuItem.SHOW_AS_ACTION_ALWAYS);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.options_menu_reload:
	            onReloadIssues();
	            return true;
	        case R.id.options_menu_restore:
	        	restorePurchises();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}



    public void restorePurchises() {
		Intent itent = new Intent(this, MainMagazineActivity.class);
		startActivity(itent);
	}

	@Override
	public void onPurchiseClick(long issueId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteClick(long issueId) {
		// TODO Auto-generated method stub
		
	}

}

