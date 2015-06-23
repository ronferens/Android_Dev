package com.jellyfish.illbeback.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CallsDBOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME	= "calls.db";
	private static final int DATABASE_VERSION	= 4;
	
	//==========================================================
	// Calls Table
	//==========================================================
	public static final String TABLE_CALLS 				= "calls";
	public static final String COLUMN_CALL_ID 			= "id";
	public static final String COLUMN_CALL_DATE 		= "date";
	public static final String COLUMN_CALL_HOUR 		= "hour";
	public static final String COLUMN_CALL_NAME 		= "name";
	public static final String COLUMN_CALL_NUMBER		= "number";
	public static final String COLUMN_CALL_DURATION 	= "duration";
	public static final String COLUMN_CALL_CONTACT_ID	= "contactID";
	public static final String COLUMN_CALL_CONTACT_URI 	= "uri";
	public static final String COLUMN_CALL_REPETITIONS 	= "repetitions";
	
	private static final String TABLE_CALLS_CREATE =
			"CREATE TABLE " + TABLE_CALLS + " (" +
					COLUMN_CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_CALL_DATE + " TEXT, " +
					COLUMN_CALL_HOUR + " TEXT, " +
					COLUMN_CALL_NAME + " TEXT, " +
					COLUMN_CALL_NUMBER + " TEXT, " +
					COLUMN_CALL_DURATION + " INTEGER, " +
					COLUMN_CALL_CONTACT_ID + " TEXT, " +
					COLUMN_CALL_CONTACT_URI + " TEXT, " +
					COLUMN_CALL_REPETITIONS + " INTEGER " +
					")";

	//==========================================================
	public CallsDBOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CALLS_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
		onCreate(db);
	}
}
