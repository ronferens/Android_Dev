package com.jellyfish.illbeback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import com.jellyfish.illbeback.data.CallItem;
import com.jellyfish.illbeback.data.CallsDataSource;
import com.jellyfish.illbeback.widget.CallsAppWidgetProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class CallDialogActivity extends Activity {

	public static final String CALL_DIALOG_ACTIVITY_CALLS_LIST 				= "callListField";
	public static final String CALL_DIALOG_ACTIVITY_IS_FIRE_LIST 			= "isFireDialogField";
	public static final String CALL_DIALOG_ACTIVITY_CALL_WAITING_LIST		= "isCallWaitingField";
	public static final String CALL_DIALOG_ACTIVITY_IS_OUTGOING_CALL_LIST	= "isOutgoingCallField";
	private static final int CALL_DIALOG_ACTIVITY_MULT_CALL_DIALOG_DELAY 	= 5000;
	static final int CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG = 0;

	private static AlertDialog mAlertDialog = null;
	private CallsDataSource mDataSource;
	private SharedPreferences mAppSettings;

	private ArrayList<CallItem> mCallsList;
	private ArrayList<CallItem> mDialogCallsList;
	private ArrayList<Integer> mDialogCallWaitingList;
	private ArrayList<Integer> mCallsListFireDialog;
	private ArrayList<Integer> mCallWaitingList;
	private ArrayList<Integer> mIsOutgoingCallList;
	private static ArrayList<Integer> mCallToAddList;
	private int mDialogDelayTime;
	private int mProgress;
	private ProgressBar mProgressBar;
	private CountDownTimer mCountDownTimer;
	private int mProgressInSec;
	private Bitmap mBitmap;
	private RoundedImageView mRoundedImage;
	private CallItem mDialogOutgoingCall;

	private boolean mIsIncomingCallsToHandle;
	private boolean mIsOutgoingCallsToHandle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	    		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.call_dialog_activity);

		mDataSource = new CallsDataSource(this);

		Intent intent = this.getIntent();
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			finish();
		}

		mCallsList = new ArrayList<CallItem>();
		mCallsListFireDialog = new ArrayList<Integer>();
		mDialogCallsList = new ArrayList<CallItem>();
		mCallWaitingList = new ArrayList<Integer>();
		mDialogCallWaitingList = new ArrayList<Integer>();
		mCallToAddList = new ArrayList<Integer>();

		mCallsList = intent.getParcelableArrayListExtra(CALL_DIALOG_ACTIVITY_CALLS_LIST);
		mCallsListFireDialog = intent.getIntegerArrayListExtra(CALL_DIALOG_ACTIVITY_IS_FIRE_LIST);
		mCallWaitingList = intent.getIntegerArrayListExtra(CALL_DIALOG_ACTIVITY_CALL_WAITING_LIST);
		mCallWaitingList = intent.getIntegerArrayListExtra(CALL_DIALOG_ACTIVITY_CALL_WAITING_LIST);
		mIsOutgoingCallList = intent.getIntegerArrayListExtra(CALL_DIALOG_ACTIVITY_IS_OUTGOING_CALL_LIST);

		mIsIncomingCallsToHandle = false;
		mIsOutgoingCallsToHandle = false;

		if (mCallsList.size() != mCallsListFireDialog.size()) {
			Toast.makeText(CallDialogActivity.this, "Call Back Later - something went wrong", Toast.LENGTH_SHORT).show();
			finish();
		}

		// Going over all input calls:
		// Handling calls that don't need dialog and listing all the ones that do need
		for (int i = 0; i < mCallsList.size(); i++) {
			if(mIsOutgoingCallList.get(i) == 0) {
				if (mCallsListFireDialog.get(i) != 0) {
					CallItem call = mCallsList.get(i);
					mDialogCallsList.add(call);
					mDialogCallWaitingList.add(mCallWaitingList.get(i));
				} else {
					insertCall(mCallsList.get(i));
					updateWidget();
				}
			} else {
				if (mCallsListFireDialog.get(i) != 0) {
					mDialogOutgoingCall = new CallItem();
					mDialogOutgoingCall = mCallsList.get(i);
				} else {
					removeCall(mCallsList.get(i));
					updateWidget();
				}
			}
		}

		if (mDialogCallsList.size() >= 1) {
			mIsIncomingCallsToHandle = true;
		}
		if (mDialogOutgoingCall != null) {
			mIsOutgoingCallsToHandle = true;
		}
		handleCalls();
	}

	private Handler mClosingHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG: {
					handleCalls();
					break;
				}

				default:
					break;
			}
		}
	};

	private void handleCalls() {
		mClosingHandler.removeMessages(CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG);
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}

		// Firing incoming calls dialog
		if(mIsIncomingCallsToHandle) {
			if (mDialogCallsList.size() == 1) {
				mAppSettings = getSharedPreferences(PreferencesActivity.APP_SETTINGS, MODE_PRIVATE);
				mDialogDelayTime = mAppSettings.getInt(PreferencesActivity.SETTING_ALERT_DIALOG_DURATION, PreferencesActivity.mAlertDialogDurationArray[0]);
				fireSingleCallDialog(mDialogCallsList.get(0), mDialogDelayTime);
			} else if (mDialogCallsList.size() > 1) {
				mDialogDelayTime = mDialogCallsList.size() * CALL_DIALOG_ACTIVITY_MULT_CALL_DIALOG_DELAY;
				fireMultiplesCallDialog(mDialogCallsList, mDialogCallWaitingList, mDialogDelayTime);
			}

			// Closing the activity after a given delay
			mIsIncomingCallsToHandle = false;

		} else if (mIsOutgoingCallsToHandle) {
			// Firing outgoing call suggestion dialog
			if (mDialogOutgoingCall != null) {
				mAppSettings = getSharedPreferences(PreferencesActivity.APP_SETTINGS, MODE_PRIVATE);
				mDialogDelayTime = 20000;
				fireOutgoingCallDialog(mDialogOutgoingCall, mDialogDelayTime);
			}

			// Closing the activity after a given delay
			mIsOutgoingCallsToHandle = false;
		} else {
			updateWidget();
			finish();
		}
	}

	public static void addItemPositionToCallToAdd(int position) {
		if (!mCallToAddList.contains(position)) {
			mCallToAddList.add(position);
		}
	}

	public static void removeItemPositionToCallToAdd(int position) {
		if (mCallToAddList.contains(position)) {
			int index = mCallToAddList.indexOf(position);
			mCallToAddList.remove(index);
		}
	}

	private void fireSingleCallDialog(final CallItem call, int dialogTimeOut) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CallDialogActivity.this);				
		alertDialogBuilder.setTitle(R.string.app_name);
		alertDialogBuilder.setIcon(R.drawable.ic_launcher);

		LayoutInflater inflater = getLayoutInflater();
		View alertView = inflater.inflate(R.layout.call_dialog, null);
		alertDialogBuilder.setView(alertView);

		alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(R.string.dialog_add_call, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						insertCall(call);
						handleCalls();;
					}
				})
				.setNegativeButton(R.string.dialog_ignore_call, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						handleCalls();
					}
				});

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				handleCalls();
			}
		});
		mAlertDialog.show();

		// Updating the contact text view
		//-----------------------------------------
		TextView numberTextView = (TextView) mAlertDialog.findViewById(R.id.contactNumbertextView);
		ImageView photo = (ImageView) mAlertDialog.findViewById(R.id.contactImageView);
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_contact_image);

		String contactID = call.getContactID();
		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			// If the number is a contact --> Show contact name
			numberTextView.setText(call.getName());

			if (call.getContactUri() != null) {
				Uri imageUri = Uri.parse(call.getContactUri());

				try {
					mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			numberTextView.setText(call.getNumber());
		}

		mRoundedImage = new RoundedImageView(mBitmap);
		photo.setImageDrawable(mRoundedImage);

		// Updating the progress Bar
		//-----------------------------------------
		TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
		mProgressInSec = mDialogDelayTime / 1000;
		tv.setText(Integer.toString(mProgressInSec));

		mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.progressBarDialog);
		mProgress = 0;
		mProgressBar.setProgress(mProgress);
		mCountDownTimer = new CountDownTimer(mDialogDelayTime, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
				mProgressInSec = (mDialogDelayTime - ((mProgress - 1) * 1000)) / 1000;
				tv.setText(Integer.toString(mProgressInSec));
			}

			@Override
			public void onFinish() {
				// Do what you want 
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
				tv.setText("1");
			}
		};
		mCountDownTimer.start();

		// Dismiss dialog in dialogTimeOut (ms)
		mClosingHandler.sendEmptyMessageDelayed(CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG, dialogTimeOut);
	}

	private void fireMultiplesCallDialog(final ArrayList<CallItem> callsList, ArrayList<Integer> callWaitingList, int dialogTimeOut) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CallDialogActivity.this);
		alertDialogBuilder.setTitle(R.string.app_name);
		alertDialogBuilder.setIcon(R.drawable.ic_launcher);

		LayoutInflater inflater = getLayoutInflater();
		View convertView = (View) inflater.inflate(R.layout.call_dialog_multiple, null);
		alertDialogBuilder.setView(convertView);

		ListView mDialogCallsListView = (ListView)convertView.findViewById(R.id.dialogCallsListView);
		ArrayAdapter<CallItem> adapter = new CallListDialogAdapter( this, R.layout.call_dialog_list_adapter, R.id.textViewDialogCallNumber, callsList, callWaitingList);
		mDialogCallsListView.setAdapter(adapter);

		alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(R.string.dialog_add_selected_calls, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						for (int i = 0; i < mCallToAddList.size(); i++) {
							insertCall(callsList.get(mCallToAddList.get(i)));
						}
						handleCalls();
					}
				})
				.setNegativeButton(R.string.dialog_ignore_call, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						handleCalls();
					}
				});

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				handleCalls();
			}
		});

		mAlertDialog.show();

		// Updating the progress Bar
		//-----------------------------------------
		TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarMultipleDialogText);
		mProgressInSec = mDialogDelayTime / 1000;
		tv.setText(Integer.toString(mProgressInSec));

		mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.progressBarMultipleDialog);
		mProgress = 0;
		mProgressBar.setProgress(mProgress);
		mCountDownTimer = new CountDownTimer(mDialogDelayTime, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarMultipleDialogText);
				mProgressInSec = (mDialogDelayTime - ((mProgress - 1) * 1000)) / 1000;
				tv.setText(Integer.toString(mProgressInSec));
			}

			@Override
			public void onFinish() {
				// Do what you want
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
				tv.setText("1");
			}
		};
		mCountDownTimer.start();

		// Dismiss dialog in dialogTimeOut (ms)
		mClosingHandler.sendEmptyMessageDelayed(CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG, dialogTimeOut);
	}

	private void fireOutgoingCallDialog(final CallItem call, int dialogTimeOut) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CallDialogActivity.this);
		alertDialogBuilder.setTitle(R.string.app_name);
		alertDialogBuilder.setIcon(R.drawable.ic_launcher);

		LayoutInflater inflater = getLayoutInflater();
		View alertView = inflater.inflate(R.layout.call_dialog, null);
		alertDialogBuilder.setView(alertView);

		alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(R.string.dialog_outgoing_call_remove, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						removeCall(call);
						handleCalls();
					}
				})
				.setNegativeButton(R.string.dialog_outgoing_call_keep, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						handleCalls();
					}
				});

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				handleCalls();
			}
		});
		mAlertDialog.show();

		// Updating the contact text view
		//-----------------------------------------
		TextView msgTextView = (TextView)mAlertDialog.findViewById((R.id.textViewMassage));
		msgTextView.setText(R.string.dialog_outgoing_call_remove_msg);

		TextView numberTextView = (TextView) mAlertDialog.findViewById(R.id.contactNumbertextView);
		ImageView photo = (ImageView) mAlertDialog.findViewById(R.id.contactImageView);
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_contact_image);

		String contactID = call.getContactID();
		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			// If the number is a contact --> Show contact name
			numberTextView.setText(call.getName());

			if (call.getContactUri() != null) {
				Uri imageUri = Uri.parse(call.getContactUri());

				try {
					mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {

					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			numberTextView.setText(call.getNumber());
		}

		mRoundedImage = new RoundedImageView(mBitmap);
		photo.setImageDrawable(mRoundedImage);

		// Updating the progress Bar
		//-----------------------------------------
		TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
		mProgressInSec = mDialogDelayTime / 1000;
		tv.setText(Integer.toString(mProgressInSec));

		mProgressBar = (ProgressBar)mAlertDialog.findViewById(R.id.progressBarDialog);
		mProgress = 0;
		mProgressBar.setProgress(mProgress);
		mCountDownTimer = new CountDownTimer(mDialogDelayTime, 1000) {

			@Override
			public void onTick(long millisUntilFinished) {
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
				mProgressInSec = (mDialogDelayTime - ((mProgress - 1) * 1000)) / 1000;
				tv.setText(Integer.toString(mProgressInSec));
			}

			@Override
			public void onFinish() {
				// Do what you want
				mProgress++;
				mProgressBar.setProgress(mProgress);

				TextView tv = (TextView) mAlertDialog.findViewById(R.id.progressBarDialogText);
				tv.setText("1");
			}
		};
		mCountDownTimer.start();

		// Dismiss dialog in dialogTimeOut (ms)
		mClosingHandler.sendEmptyMessageDelayed(CALL_DIALOG_ACTIVITY_MSG_DISMISS_DIALOG, dialogTimeOut);
	}

	private void insertCall(CallItem call) {
		mDataSource.open();
		mDataSource.insertCall(call);
	}

	private void removeCall(CallItem call) {
		mDataSource.open();
		mDataSource.removeCall(call);
	}

	private void updateWidget() {
		Intent intent = new Intent(this,CallsAppWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CallsAppWidgetProvider.class));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	}
}