package com.jellyfish.illbeback.widget;



import android.content.Intent;
import android.widget.RemoteViewsService;

public class WidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		RemoteViewsFactory listProvider = new CallsListRemoteViewsFactory(this.getApplicationContext(), intent);
		return listProvider;
	}

}
