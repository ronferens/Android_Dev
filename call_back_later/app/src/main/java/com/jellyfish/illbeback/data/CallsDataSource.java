package com.jellyfish.illbeback.data;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CallsDataSource {

	SQLiteOpenHelper dbHelpler;
	SQLiteDatabase dataBase;
	
	//======================================================
	//	Calls Table
	//======================================================
	private static final String[] callsTable_allColumns = {
		CallsDBOpenHelper.COLUMN_CALL_ID,
		CallsDBOpenHelper.COLUMN_CALL_DATE,
		CallsDBOpenHelper.COLUMN_CALL_HOUR,
		CallsDBOpenHelper.COLUMN_CALL_NAME,
		CallsDBOpenHelper.COLUMN_CALL_NUMBER,
		CallsDBOpenHelper.COLUMN_CALL_DURATION,
		CallsDBOpenHelper.COLUMN_CALL_CONTACT_ID,
		CallsDBOpenHelper.COLUMN_CALL_CONTACT_URI,
		CallsDBOpenHelper.COLUMN_CALL_REPETITIONS };
	
	//======================================================

	public CallsDataSource(Context context) {
		dbHelpler = new CallsDBOpenHelper(context);
	}
	
	public void open() {
		dataBase = dbHelpler.getWritableDatabase();
	}

	public void close() {
		dbHelpler.close();
	}
	
	public CallItem insertCall(CallItem item) {
		Long id = isCallExist(item);
		if (id.equals(CallItem.CALL_ID_UNKOWN)) {
			item = createCall(item);
		} else {
			item.setId(id);
			updateCall(item);
		}
		
		return item;
	}
	
	public boolean removeCall(CallItem item) {
		Long id = isCallExist(item);
		if (!id.equals(CallItem.CALL_ID_UNKOWN)) {
			String where = CallsDBOpenHelper.COLUMN_CALL_ID + "=" + id;
			int result = dataBase.delete(CallsDBOpenHelper.TABLE_CALLS, where, null);
			return (result == 1);
		}

		return false;
	}
	
	public void removeAllCalls() {
		dataBase.delete(CallsDBOpenHelper.TABLE_CALLS, null, null);
	}
	
	public int getNumberOfCalls() {
		List<CallItem> calls = findAllCalls();
		return calls.size();
	}
	
	public List<CallItem> findAllCalls() {
		// Retrieving all item in the calls' list
		String orderBy = CallsDBOpenHelper.COLUMN_CALL_DATE + " DESC, " + CallsDBOpenHelper.COLUMN_CALL_HOUR + " DESC";
		Cursor cursor = dataBase.query(CallsDBOpenHelper.TABLE_CALLS, callsTable_allColumns,
				null, null, null, null, orderBy);

		List<CallItem> calls = cursorToCallItemList(cursor);
		return calls;
	}
	
	public boolean DeleteCallsByDates(String endDate) {
		String selection = CallsDBOpenHelper.COLUMN_CALL_DATE + " < ?";
		String[] selectionArgs = {endDate};		
		int result = dataBase.delete(CallsDBOpenHelper.TABLE_CALLS, selection, selectionArgs);
		return (result == 1);
	}
		
	private CallItem createCall(CallItem item) {
		ContentValues values = extractValuesFromCallItem(item);
		long insertID = dataBase.insert(CallsDBOpenHelper.TABLE_CALLS, null, values);
		item.setId(insertID);
		return item;
	}
	
	private ContentValues extractValuesFromCallItem(CallItem item) {
		ContentValues values = new ContentValues();
		values.put(CallsDBOpenHelper.COLUMN_CALL_DATE, item.getDate());
		values.put(CallsDBOpenHelper.COLUMN_CALL_HOUR, item.getHour());
		values.put(CallsDBOpenHelper.COLUMN_CALL_NAME, item.getName());
		values.put(CallsDBOpenHelper.COLUMN_CALL_NUMBER, item.getNumber());
		values.put(CallsDBOpenHelper.COLUMN_CALL_DURATION, item.getDuration());
		values.put(CallsDBOpenHelper.COLUMN_CALL_CONTACT_ID, item.getContactID());
		values.put(CallsDBOpenHelper.COLUMN_CALL_CONTACT_URI, item.getContactUri());
		values.put(CallsDBOpenHelper.COLUMN_CALL_REPETITIONS, item.getRepetitions());
		return values;
	}
	
	private boolean updateCall(CallItem item) {
		String where = CallsDBOpenHelper.COLUMN_CALL_ID + "=" + item.getId();
		ContentValues values = extractValuesFromCallItem(item);
		
		// Increasing the number of repetition
		int repetitions = getCallRepetitions(item);
		values.put(CallsDBOpenHelper.COLUMN_CALL_REPETITIONS, (repetitions + 1));
		
		int result = dataBase.update(CallsDBOpenHelper.TABLE_CALLS, values, where, null);
		return (result == 1);
	}
		
	public Long isCallExist(CallItem item) {
		String query = "select * from " + CallsDBOpenHelper.TABLE_CALLS + " where " + CallsDBOpenHelper.COLUMN_CALL_NUMBER + "=\""+ item.getNumber() + "\"";
		Cursor cursor = dataBase.rawQuery(query, null);

		if (cursor != null && cursor.moveToFirst()) {
			// record exists
			return cursor.getLong(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_ID));
		}
		
		// record not found
		return CallItem.CALL_ID_UNKOWN;
	}
	
	private int getCallRepetitions(CallItem item) {
		String query = "select * from " + CallsDBOpenHelper.TABLE_CALLS + " where " + CallsDBOpenHelper.COLUMN_CALL_NUMBER + "=\""+ item.getNumber() + "\"";
		Cursor cursor = dataBase.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			// record exists
			return cursor.getInt(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_REPETITIONS));
		}
		
		// record not found
		return 1;
	}

	private List<CallItem> cursorToCallItemList(Cursor cursor) {
		List<CallItem> calls = new ArrayList<CallItem>();
		if (cursor != null && cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				CallItem call = new CallItem();
				cursorToCallItem(cursor, call);
				calls.add(call);
			}
		}
		return calls;
	}

	private void cursorToCallItem(Cursor cursor, CallItem call) {
		call.setId(cursor.getLong(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_ID)));
		call.setDate(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_DATE)));
		call.setHour(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_HOUR)));
		call.setName(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_NAME)));
		call.setNumber(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_NUMBER)));
		call.setDuration(cursor.getLong(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_DURATION)));
		call.setContactID(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_CONTACT_ID)));
		call.setContactUri(cursor.getString(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_CONTACT_URI)));
		call.setRepetitions(cursor.getInt(cursor.getColumnIndex(CallsDBOpenHelper.COLUMN_CALL_REPETITIONS)));
	}
}
