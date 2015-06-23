package com.jellyfish.illbeback.widget;

import java.util.List;
import com.jellyfish.illbeback.R;
import com.jellyfish.illbeback.data.CallItem;
import com.jellyfish.illbeback.data.CallsDataSource;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class CallsListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	
	private CallsDataSource mDataSource;
	private List<CallItem> mCallsList;
	private Context mContext = null;

	public CallsListRemoteViewsFactory(Context context, Intent intent) {
		this.mContext = context;
	}

	@Override
	public int getCount() {
		return mCallsList.size();
	}

	@Override
	public long getItemId(int position) {
		return mCallsList.get(position).getId();
	}

	// Similar to getView of Adapter where instead of View - we return RemoteViews
	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_row_layout);
		CallItem callItem = mCallsList.get(position);
	
		// Setting the call's name/number
		String contactID = callItem.getContactID();
		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			row.setTextViewText(R.id.textViewWidgetContactName, callItem.getName());
		} else {
			row.setTextViewText(R.id.textViewWidgetContactName, callItem.getNumber());			
		}
		
		// Setting the call's repetitions
		int callRepetitions = callItem.getRepetitions();
		if (callRepetitions == 1) {
			row.setTextViewText(R.id.textViewWidgetCallRepetitions, "");
		} else {
			row.setTextViewText(R.id.textViewWidgetCallRepetitions, ("(" + callRepetitions + ")"));			
		}
		
		// Setting the call's date
		row.setTextViewText(R.id.textWidgetViewCallDate, callItem.getDate());
		
		Intent fillInIntent = new Intent();
        fillInIntent.putExtra(CallsAppWidgetProvider.EXTRA_ITEM_LIST_VIEW_ROW_NUMBER, position);
        row.setOnClickFillInIntent(R.id.textViewWidgetContactName, fillInIntent);

		return row;
	}

	@Override
	public RemoteViews getLoadingView() {
		return null;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public void onCreate() {
		mDataSource = new CallsDataSource(mContext);
		mDataSource.open();
		mCallsList = mDataSource.findAllCalls();
	}

	@Override
	public void onDataSetChanged() {
		mDataSource = new CallsDataSource(mContext);
		mDataSource.open();
		mCallsList = mDataSource.findAllCalls();
	}

	@Override
	public void onDestroy() {
	}
}
