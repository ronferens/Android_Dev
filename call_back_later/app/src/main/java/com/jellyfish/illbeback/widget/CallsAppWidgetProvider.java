package com.jellyfish.illbeback.widget;

import com.jellyfish.illbeback.Main;
import com.jellyfish.illbeback.R;
import com.jellyfish.illbeback.data.CallsDataSource;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class CallsAppWidgetProvider extends AppWidgetProvider {

	public static final String ITEM_SELECTED_ACTION = "ITEM_SELECTED_ACTION";
    public static final String EXTRA_ITEM_LIST_VIEW_ROW_NUMBER = "EXTRA_ITEM_LIST_VIEW_ROW_NUMBER";
	
	private CallsDataSource mDataSource;
	private Context mContext;
	
	@Override
    public void onReceive(Context context, Intent intent) {
		mContext = context;
		
		if (intent.getAction().equals(ITEM_SELECTED_ACTION)) {
        	mDataSource = new CallsDataSource(mContext);
    		mDataSource.open();
        	
            int position = intent.getIntExtra(EXTRA_ITEM_LIST_VIEW_ROW_NUMBER, -1);
            if (position >= 0) {
            	Intent activityIntent = new Intent(mContext, Main.class);
            	activityIntent.setAction(Main.CONTACT_ITEM_IN_LIST_ACTION);
            	activityIntent.putExtra(Main.WIDGET_ITEM_SELECTED_POSITION, position);
            	activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            	mContext.startActivity(activityIntent);
			}
        }
        
		AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
		int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
		mgr.notifyAppWidgetViewDataChanged(widgetID, R.id.listViewWidget);
        
        super.onReceive(mContext, intent);
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public void onUpdate(Context context, AppWidgetManager
			appWidgetManager,int[] appWidgetIds) {
		mContext = context;		
		
		mDataSource = new CallsDataSource(context);
		mDataSource.open();
		
		for (int i = 0; i < appWidgetIds.length; i++) {
			// RemoteViews Service needed to provide adapter for ListView
			Intent serviceIntent = new Intent(context, WidgetService.class);

			// Passing App widget id to that RemoteViews Service
			// and setting a unique Uri to the intent
			serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

			// Which layout to show on widget
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

			//----------------------------------
			// List View
			//----------------------------------
			// Setting the adapter for listview of the widget
			remoteViews.setRemoteAdapter(appWidgetIds[i], R.id.listViewWidget, serviceIntent);
			
			// Setting an empty view in case of no data
			remoteViews.setEmptyView(R.id.listViewWidget, R.id.empty_view);
			
			Intent callIntent = new Intent(context, CallsAppWidgetProvider.class);
			callIntent.setAction(CallsAppWidgetProvider.ITEM_SELECTED_ACTION);
			callIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			callIntent.setData(Uri.parse(callIntent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent callPendingIntent = PendingIntent.getBroadcast(context, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setPendingIntentTemplate(R.id.listViewWidget, callPendingIntent);

			//----------------------------------
			// Application Launcher
			//----------------------------------
			// When we click the widget, we want to open our main activity.
		    Intent launchActivity = new Intent(context, Main.class);
		    launchActivity.setAction(Main.WIDGET_LAUNCH_APP_ACTION);
		    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchActivity, 0);
		    remoteViews.setOnClickPendingIntent(R.id.imageViewAppLauncher, pendingIntent);
		    
		    //----------------------------------
			// Number of Calls Text
			//----------------------------------   
		    String numOfCallsText;
		    int numOFCalls = mDataSource.getNumberOfCalls();
		    if (numOFCalls == 0) {
		    	numOfCallsText = "No Calls";
			} else if (numOFCalls == 1) {
		    	numOfCallsText = numOFCalls + " Call";
			} else {
		    	numOfCallsText = numOFCalls + " Calls";
		    }		    
		    remoteViews.setTextViewText(R.id.textViewNunberOfCalls, numOfCallsText);

		    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			mgr.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.listViewWidget);
		    
			appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);
		}

		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}