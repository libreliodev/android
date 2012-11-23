package com.librelio.lib.ui;

import java.util.ArrayList;

import com.librelio.lib.utils.cloud.CloudHelper;
import com.librelio.lib.utils.cloud.Issue;
import com.librelio.wind.R;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class IssueListAdapter extends SimpleCursorAdapter {
	@SuppressWarnings("unused")
	private static final String TAG = IssueListAdapter.class.getSimpleName();
	
	private CloudHelper cloud;
	ArrayList<Issue> mIssuesArrayList;
	LayoutInflater inflater;
	IssueListEventListener listener;
	SimpleCursorAdapter.ViewBinder binder;
	
	
//	public IssueListAdapter(Context context, CloudHelper cloud, IssueListEventListener listener){
//		this.cloud = cloud;
//		this.mIssuesArrayList = cloud.getIssueList();
//		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		this.listener = listener;
//	}
	
	
	public IssueListAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, CloudHelper cl, final IssueListEventListener listener) {
		super(context, layout, c, from, to);
		this.cloud = cl;
		this.listener = listener;
		final boolean isSubscribed = cloud.isSubscribtionActive();
		SimpleCursorAdapter.ViewBinder binder = new SimpleCursorAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view,  Cursor cursor, int columnIndex) {
				final long issueId = cursor.getLong(0);
				long id = cursor.getLong(0);
				Issue issue = cloud.getIssue(id);
				switch (view.getId()){
				case R.id.issue_cover:{
					ImageView iv = (ImageView) view;
					iv.setImageDrawable(Drawable.createFromPath(cursor.getString(columnIndex)));
					iv.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View arg0) {
							listener.onReadClick(issueId);
						}
					});
					return true;
				}
				case R.id.issue_title:{
					TextView tv = (TextView) view;
					tv.setText(cursor.getString(columnIndex));
					return true;
				}
				case R.id.issue_button:{
					Button b = (Button) view;
					
					
					
					if(isSubscribed){
						if((issue.getState() & Issue.STATE_LOADED) == 0){
							// Subscribed but not loaded

							b.setText(R.string.download);
							b.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View arg0) {
									
									listener.onDownloadClick(issueId);
								}});

							
						} else {
							// Subscribed and loaded
							
							b.setText(R.string.delete);
							b.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View arg0) {
									
									listener.onDeleteClick(issueId);
								}});					
							}
					}
					if((issue.getState() & Issue.STATE_PURCHASED) == 0){
						// This issue has not has been purchased yet
						// Show purchise option only
						
						b.setText(R.string.purchise);
						b.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								listener.onPurchiseClick(issueId);
								
							}});
						
					}else if((issue.getState() & Issue.STATE_LOADED) == 0){
						// Issue has been purchased but not loaded

						b.setText(R.string.download);
						b.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								
								listener.onDownloadClick(issueId);
							}});

						
					} else {
						// Issue has been purchased and loaded
						
						b.setText(R.string.delete);
						b.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								
								listener.onDeleteClick(issueId);
							}});					
						}
					return true;
				}
				}
				return false;
			}
		};
		setViewBinder(binder);
	}

	public interface IssueListEventListener {
		public void onReadClick(long issueId);
		public void onPurchiseClick(long issueId);
		public void onDownloadClick(long  issueId);
		public void onDeleteClick(long issueId);
	}
	
}
