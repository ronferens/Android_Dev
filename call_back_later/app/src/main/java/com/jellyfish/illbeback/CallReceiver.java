package com.jellyfish.illbeback;

import java.util.ArrayList;
import com.jellyfish.illbeback.data.CallItem;
import com.jellyfish.illbeback.data.CallsDataSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {

	private static final String CALL_RECEIVER_PREFS					= "callReceiverPrefs";
	private static final String CALL_RECEIVER_INCOMING_NUMBER		= "incomingNumber";
	private static final String CALL_RECEIVER_OUTGOING_NUMBER		= "outgoingNumber";

	private static final String CALL_RECEIVER_IS_INCOMING_CALL		= "isIncomingCall";
	private static final String CALL_RECEIVER_IS_OUTGOING_CALL		= "isOutgoingCall";
	private static final String CALL_RECEIVER_IS_CALL_ANSWERED		= "isCallAnswered";

	private static final String CALL_RECEIVER_CALL_START_TIME		= "callStartTime";
	private static final String CALL_RECEIVER_CALL_WAITING_NUMBERS	= "callWaitingNumbers";
	private static final String CALL_RECEIVER_CALL_WAITING_DETECTED	= "callWaitingDetect";

	private static final String CALL_RECEIVER_PREVIOUS_STATE			= "previousState";
	private static final String CALL_RECEIVER_PREVIOUS_STATE_IDLE		= "IDLE";
	private static final String CALL_RECEIVER_PREVIOUS_STATE_RINGING	= "RINGING";
	private static final String CALL_RECEIVER_PREVIOUS_STATE_OFFHOOK	= "OFFHOOK";

	private SharedPreferences mAppSettings;
	private SharedPreferences mSettings;
	private CallsDataSource mDataSource;
	private String mPreviousState;
	private String mCurrentState;
	private SharedPreferences.Editor mEditor;
	private String mIncomingNumber;
	private String mOutGoingNumber;

	private Long mCallEndTime;
	private Long mCallStartTime;

	private Context mContext;

	@Override
	public void onReceive(Context context, Intent intent) {

		mContext = context;
		mAppSettings = context.getSharedPreferences(PreferencesActivity.APP_SETTINGS, Context.MODE_PRIVATE);
		mSettings = context.getSharedPreferences(CALL_RECEIVER_PREFS, Context.MODE_PRIVATE);
		mEditor = mSettings.edit();

		mPreviousState = mSettings.getString(CALL_RECEIVER_PREVIOUS_STATE, CALL_RECEIVER_PREVIOUS_STATE_IDLE);
		mIncomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

		mDataSource = new CallsDataSource(context);

		//==============================================
		// Examining intent type and acting accordingly
		//==============================================
		if(Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
			// Saving the outgoing call number
			mOutGoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			mEditor.putString(CALL_RECEIVER_OUTGOING_NUMBER, mOutGoingNumber);
			mEditor.commit();
			return;
		} else
		{
			// Retrieving the phone state
			mCurrentState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if (mCurrentState == null) {
				return;
			}
		}


		if (mCurrentState.equals(TelephonyManager.EXTRA_STATE_RINGING) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_IDLE)) {

			//==============================================
			// Handling Incoming Call
			//==============================================

			// Saving the current state + incoming call + call NOT answered
			mEditor.putString(CALL_RECEIVER_PREVIOUS_STATE, mCurrentState);
			mEditor.putString(CALL_RECEIVER_INCOMING_NUMBER, mIncomingNumber);
			mEditor.putBoolean(CALL_RECEIVER_IS_OUTGOING_CALL, false);
			mEditor.putBoolean(CALL_RECEIVER_IS_INCOMING_CALL, true);
			mEditor.putBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, false);
			mEditor.commit();

		} else if (mCurrentState.equals(TelephonyManager.EXTRA_STATE_RINGING) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_OFFHOOK)) {

			//==============================================
			// Handling Call Waiting
			//==============================================

			if (mIncomingNumber != null) {
				// Saving call waiting number
				String callWaitingNumbers = mSettings.getString(CALL_RECEIVER_CALL_WAITING_NUMBERS, "");
				boolean isNumListedAlready = callWaitingNumbers.contains(mIncomingNumber);
				if (!isNumListedAlready) {
					if (callWaitingNumbers.length() != 0) {
						callWaitingNumbers += ",";
					}
					callWaitingNumbers += mIncomingNumber;
					mEditor.putString(CALL_RECEIVER_CALL_WAITING_NUMBERS, callWaitingNumbers);							
				}

				// Indicating call waiting detected + call waiting numbers
				mEditor.putBoolean(CALL_RECEIVER_CALL_WAITING_DETECTED, true);
				mEditor.commit();
			}


		} else if (mCurrentState.equals(TelephonyManager.EXTRA_STATE_IDLE) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_RINGING)) {

			//==============================================
			// Handling Rejected Call
			//==============================================

			// Setting the call start time
			mCallStartTime = System.currentTimeMillis();

			// Saving the current state
			mEditor.putString(CALL_RECEIVER_PREVIOUS_STATE, mCurrentState);
			mEditor.putLong(CALL_RECEIVER_CALL_START_TIME, mCallStartTime);
			mEditor.putBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, false);
			mEditor.commit();

			processCalls(context);


		} else if(mCurrentState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_RINGING) ) {

			//==============================================
			// Handling Answered Incoming Call
			//==============================================

			// Saving the call start time
			mCallStartTime = System.currentTimeMillis();

			// Saving the current state
			mEditor.putString(CALL_RECEIVER_PREVIOUS_STATE, mCurrentState);
			mEditor.putLong(CALL_RECEIVER_CALL_START_TIME, mCallStartTime);
			mEditor.putBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, true);
			mEditor.commit();

		} else if(mCurrentState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_IDLE) ) {

			//==============================================
			// Handling Outgoing Call
			//==============================================

			// Saving the current state
			mEditor.putString(CALL_RECEIVER_PREVIOUS_STATE, mCurrentState);
			mEditor.putBoolean(CALL_RECEIVER_IS_OUTGOING_CALL, true);
			mEditor.putBoolean(CALL_RECEIVER_IS_INCOMING_CALL, false);
			mEditor.putBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, false);
			mEditor.commit();

		} else if(mCurrentState.equals(TelephonyManager.EXTRA_STATE_IDLE) &&
				mPreviousState.equals(CALL_RECEIVER_PREVIOUS_STATE_OFFHOOK)) {

			//==============================================
			// Handling Disconnected Call
			//==============================================

			// Saving the current state
			mEditor.putString(CALL_RECEIVER_PREVIOUS_STATE, mCurrentState);
			mEditor.commit();

			processCalls(context);
		}

	}

	private void processCalls(Context context) {

		// Calculating call duration
		mCallEndTime = System.currentTimeMillis();
		mCallStartTime = mSettings.getLong(CALL_RECEIVER_CALL_START_TIME, mCallEndTime);
		long callDuration = mCallEndTime - mCallStartTime;

		// Retrieving main call's parameters
		boolean isIncomingCall = mSettings.getBoolean(CALL_RECEIVER_IS_INCOMING_CALL, false);
		boolean isOutgoingCall = mSettings.getBoolean(CALL_RECEIVER_IS_OUTGOING_CALL, false);
		boolean isCallAnswered = mSettings.getBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, false);
		String incomingNumber = mSettings.getString(CALL_RECEIVER_INCOMING_NUMBER, null);
		String outgoingNumber = mSettings.getString(CALL_RECEIVER_OUTGOING_NUMBER, null);

		ArrayList<CallItem> intentCallsList = new ArrayList<CallItem>();
		ArrayList<Integer> intentFireDialogList = new ArrayList<Integer>();
		ArrayList<Integer> intentCallWaitingList = new ArrayList<Integer>();
		ArrayList<Integer> intentIsOutgoingCallList = new ArrayList<Integer>();

		//---------------------------------------------------------------
		// Listing all missed calls (call waiting) during main call
		//---------------------------------------------------------------
		boolean validCallWaiting;
		boolean isfireDialogCallWaiting;

		boolean isWaitingCallDetected = mSettings.getBoolean(CALL_RECEIVER_CALL_WAITING_DETECTED, false);
		if (isWaitingCallDetected) {
			validCallWaiting = isHandleIncomingCall(context, true, false);
			if (validCallWaiting) {
				isfireDialogCallWaiting = isFireIncomingCallDialog(context, true, false);
				String listedNumbers = mSettings.getString(CALL_RECEIVER_CALL_WAITING_NUMBERS, null);
				if (listedNumbers != null) {
					String[] missedCallsNumbers = listedNumbers.split(",");
					for (int i = 0; i < missedCallsNumbers.length; i++) {
						CallItem call = ContactUtils.fillCallDetails(mContext, missedCallsNumbers[i], 0);
						intentCallsList.add(call);
						intentFireDialogList.add(Integer.valueOf(Utils.boolToInt(isfireDialogCallWaiting)));
						intentCallWaitingList.add(Integer.valueOf(Utils.boolToInt(true)));
						intentIsOutgoingCallList.add(Integer.valueOf(Utils.boolToInt(false)));
					}
				}
			}
		}

		//---------------------------------------------------------------
		// Listing main call
		//---------------------------------------------------------------
		String callMonitorType = mAppSettings.getString(PreferencesActivity.SETTING_CALL_MONITOR_TYPE, PreferencesActivity.mAnsweredCallsArray[1]);
		String durationTh = mAppSettings.getString(PreferencesActivity.SETTING_ANSWERED_CALL_THRESHOLD, PreferencesActivity.DEFAULT_CALL_DURATION_THRESHOLD);
		long callDurationThreshold = Long.valueOf(durationTh).longValue();

		if(isOutgoingCall && (outgoingNumber != null)) {
			CallItem call = ContactUtils.fillCallDetails(mContext, outgoingNumber, callDuration);
			if(isHandleOutgoingCall(context, call)) {
				intentCallsList.add(call);
				intentFireDialogList.add(Integer.valueOf(Utils.boolToInt(isFireOutgoingCallDialog(context))));
				intentIsOutgoingCallList.add(Integer.valueOf(Utils.boolToInt(true)));
				intentCallWaitingList.add(Integer.valueOf(Utils.boolToInt(false)));
			}
		} else {
			if (callMonitorType.equals(PreferencesActivity.mAnsweredCallsArray[2]) ||
					(callMonitorType.equals(PreferencesActivity.mAnsweredCallsArray[1]) && (callDuration < callDurationThreshold))) {

				// Popping dialog only if call is within call preferred duration
				boolean validCall = isHandleIncomingCall(context, isIncomingCall, isCallAnswered);
				if (validCall && (incomingNumber != null)) {
					CallItem call = ContactUtils.fillCallDetails(mContext, incomingNumber, callDuration);
					intentCallsList.add(call);
					boolean isfireDialog = isFireIncomingCallDialog(context, isIncomingCall, isCallAnswered);
					intentFireDialogList.add(Integer.valueOf(Utils.boolToInt(isfireDialog)));
					intentIsOutgoingCallList.add(Integer.valueOf(Utils.boolToInt(false)));
					intentCallWaitingList.add(Integer.valueOf(Utils.boolToInt(false)));
				}

			}
		}

		//---------------------------------------------------------------
		// Invoking activity in case there where calls to handle
		//---------------------------------------------------------------
		if (!intentCallsList.isEmpty()) {
			Intent dialogIntent = new Intent(context, CallDialogActivity.class);
			dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			dialogIntent.putParcelableArrayListExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_CALLS_LIST, intentCallsList);
			dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_IS_FIRE_LIST, intentFireDialogList);
			dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_IS_OUTGOING_CALL_LIST, intentIsOutgoingCallList);
			dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_CALL_WAITING_LIST, intentCallWaitingList);
			context.startActivity(dialogIntent);
		}

		// Resetting indicators
		mEditor.putString(CALL_RECEIVER_INCOMING_NUMBER, null);
		mEditor.putString(CALL_RECEIVER_OUTGOING_NUMBER, null);
		mEditor.putBoolean(CALL_RECEIVER_IS_INCOMING_CALL, false);
		mEditor.putBoolean(CALL_RECEIVER_IS_OUTGOING_CALL, false);
		mEditor.putBoolean(CALL_RECEIVER_IS_CALL_ANSWERED, false);
		mEditor.putLong(CALL_RECEIVER_CALL_START_TIME, 0);
		mEditor.putBoolean(CALL_RECEIVER_CALL_WAITING_DETECTED, false);
		mEditor.putString(CALL_RECEIVER_CALL_WAITING_NUMBERS, "");
		mEditor.commit();
	}

	private boolean isHandleIncomingCall(Context context, boolean isIncomingCall, boolean isCallAnswered) {
		mAppSettings = context.getSharedPreferences(PreferencesActivity.APP_SETTINGS, Context.MODE_PRIVATE);
		String unansweredCallsConfig = mAppSettings.getString(PreferencesActivity.SETTING_UNANSWERED_CALLS, PreferencesActivity.mUnansweredCallsArray[2]);

		boolean isMonitorUnansweredCalls = false;
		if (!unansweredCallsConfig.equals(PreferencesActivity.mUnansweredCallsArray[0])) {
			isMonitorUnansweredCalls = true;
		}

		if (isMonitorUnansweredCalls && isIncomingCall) {
			return true;
		} else if (isIncomingCall && isCallAnswered) {			
			return true;
		} else {
			return false;			
		}		
	}

	private boolean isFireIncomingCallDialog(Context context, boolean isIncomingCall, boolean isCallAnswered) {
		mAppSettings = context.getSharedPreferences(PreferencesActivity.APP_SETTINGS, Context.MODE_PRIVATE);
		String unansweredCallsConfig = mAppSettings.getString(PreferencesActivity.SETTING_UNANSWERED_CALLS, PreferencesActivity.mUnansweredCallsArray[2]);

		boolean isAutoAddUnansweredCalls = false;
		if (unansweredCallsConfig.equals(PreferencesActivity.mUnansweredCallsArray[2])) {
			isAutoAddUnansweredCalls = true;
		}

		if (isAutoAddUnansweredCalls && isIncomingCall && !isCallAnswered) {
			return false;
		} else {
			return true;			
		}	
	}

	private boolean isHandleOutgoingCall(Context context, CallItem call) {
		// Checking the user's prefs.
		mAppSettings = context.getSharedPreferences(PreferencesActivity.APP_SETTINGS, Context.MODE_PRIVATE);
		String outgoingCallsConfig = mAppSettings.getString(PreferencesActivity.SETTING_OUTGOING_CALL_DELETION_TYPE, PreferencesActivity.mOutgoingCallDeletionArray[2]);

		// Checking if the outgoing call is in the list
		mDataSource.open();
		Long callID = mDataSource.isCallExist(call);

		if ((callID == CallItem.CALL_ID_UNKOWN) || outgoingCallsConfig.equals(PreferencesActivity.mOutgoingCallDeletionArray[2])) {
			return false;
		}

		return true;
	}

	private boolean isFireOutgoingCallDialog(Context context) {
		mAppSettings = context.getSharedPreferences(PreferencesActivity.APP_SETTINGS, Context.MODE_PRIVATE);
		String outgoingCallsConfig = mAppSettings.getString(PreferencesActivity.SETTING_OUTGOING_CALL_DELETION_TYPE, PreferencesActivity.mOutgoingCallDeletionArray[1]);
		if (outgoingCallsConfig.equals(PreferencesActivity.mOutgoingCallDeletionArray[1])) {
			return true;
		}

		return false;
	}
}
