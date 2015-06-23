package com.jellyfish.illbeback;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

public class PreferencesActivity extends Activity {

	public static final String APP_SETTINGS 						= "APP_SETTINGS";
	public static final String APP_SETTINGS_FIRST_LAUNCH			= "APP_SETTINGS_FIRST_LAUNCH";
	public static final String SETTING_CALL_MONITOR_TYPE			= "callMonitorType";
	public static final String SETTING_ANSWERED_CALL_THRESHOLD		= "answeredCallThreshold";
	public static final String SETTING_ALERT_DIALOG_DURATION		= "alertDialogDuration";
	public static final String SETTING_CALL_DELETION_TYPE			= "callDeletion";
	public static final String SETTING_UNANSWERED_CALLS				= "unansweredCalls";
	public static final String SETTING_OUTGOING_CALL_DELETION_TYPE = "outgoingCallDeletion";

	private SharedPreferences mSettings;
	private SharedPreferences.Editor mEditor;
	
	private static AlertDialog mAlertDialog = null;
	private NumberPicker mThresholdPicker = null;

	public static final String DEFAULT_CALL_DURATION_THRESHOLD = "30000";
	public static final Integer[] mAlertDialogDurationArray = {5000, 7000, 10000};
	public static final String[] mAnsweredCallsArray = {"IGNORE", "THRESHOLD", "ALL"};
	public static final String[] mCallDeletionArray = { "ALWAYS", "ASK ME", "NEVER" };
	public static final String[] mUnansweredCallsArray = { "IGNORE", "MANUALLY", "AUTO" };
	public static final String[] mOutgoingCallDeletionArray = { "AUTO", "ASK ME", "NEVER" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences_activity);
		setTitle(R.string.pref_header);

		mSettings = getSharedPreferences(APP_SETTINGS, MODE_PRIVATE);
		mEditor = mSettings.edit();

		boolean isFirstLaunch = mSettings.getBoolean(APP_SETTINGS_FIRST_LAUNCH, true);
		if (isFirstLaunch) {
			mEditor.putBoolean(APP_SETTINGS_FIRST_LAUNCH, false);
			mEditor.putInt(SETTING_ALERT_DIALOG_DURATION, mAlertDialogDurationArray[1]); //=7 Seconds
			mEditor.putString(SETTING_CALL_MONITOR_TYPE, mAnsweredCallsArray[1]); //=Set by threshold
			mEditor.putString(SETTING_ANSWERED_CALL_THRESHOLD, DEFAULT_CALL_DURATION_THRESHOLD); //=Default Threshold
			mEditor.putString(SETTING_CALL_DELETION_TYPE, mCallDeletionArray[0]); //=Auto Remove
			mEditor.putString(SETTING_UNANSWERED_CALLS, mUnansweredCallsArray[2]); //=Auto
			mEditor.putString(SETTING_OUTGOING_CALL_DELETION_TYPE, mOutgoingCallDeletionArray[0]); //=Auto Remove
			mEditor.commit();
		}

		//==================================================
		// Alert Dialog Duration Spinner

		Spinner alertDialogDurationSpinner = (Spinner) findViewById(R.id.spinnerAlertDialogDuration);
		int alertDialogDuration = mSettings.getInt(SETTING_ALERT_DIALOG_DURATION, mAlertDialogDurationArray[0]);
		int index = Arrays.asList(mAlertDialogDurationArray).indexOf(alertDialogDuration);
		if (index != -1) {
			alertDialogDurationSpinner.setSelection(index);
		} else {
			alertDialogDurationSpinner.setSelection(0);
		}

		alertDialogDurationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {		
				// Saving user's preference for call monitoring				
				mEditor.putInt(SETTING_ALERT_DIALOG_DURATION, mAlertDialogDurationArray[pos]);
				mEditor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {				
			}
		});

		//==================================================
		// Calls' Monitor Types Spinner

		Spinner callMonitorSpinner = (Spinner) findViewById(R.id.spinnerCallDuration);
		String callMonitorType = mSettings.getString(SETTING_CALL_MONITOR_TYPE, mAnsweredCallsArray[1]);
		index = Arrays.asList(mAnsweredCallsArray).indexOf(callMonitorType);
		if (index != -1) {
			callMonitorSpinner.setSelection(index);

			TextView tv = (TextView) findViewById(R.id.textViewCallDurationThreshold);
			ImageButton bt	= (ImageButton) findViewById(R.id.imageButtonSetCallDurationThreshold);
			if (index == 1) {
				bt.setEnabled(true);
				tv.setEnabled(true);
			} else {
				bt.setEnabled(false);
				tv.setEnabled(false);
			}

		} else {
			callMonitorSpinner.setSelection(0);
		}

		callMonitorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {		
				// Saving user's preference for call monitoring				
				mEditor.putString(SETTING_CALL_MONITOR_TYPE, mAnsweredCallsArray[pos]);
				mEditor.commit();

				TextView tv = (TextView) findViewById(R.id.textViewCallDurationThreshold);
				ImageButton bt	= (ImageButton) findViewById(R.id.imageButtonSetCallDurationThreshold);
				if (pos == 1) {
					bt.setEnabled(true);
					tv.setEnabled(true);
				} else {
					bt.setEnabled(false);
					tv.setEnabled(false);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {				
			}
		});

		//==================================================
		// Calls' Duration Threshold

		updateCallDurationTextView();
		
		ImageButton btCallDurationThreshold	= (ImageButton) findViewById(R.id.imageButtonSetCallDurationThreshold);
		btCallDurationThreshold.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PreferencesActivity.this);				
				alertDialogBuilder.setTitle(R.string.app_name);
				alertDialogBuilder.setIcon(R.drawable.ic_launcher);

				LayoutInflater inflater = getLayoutInflater();
				View alertView = inflater.inflate(R.layout.call_duration_dialog, null);
				alertDialogBuilder.setView(alertView);

				alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(R.string.dialog_call_duration_set, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						
						mThresholdPicker = (NumberPicker) mAlertDialog.findViewById(R.id.numberPickerCallDuratrion);
						int th = mThresholdPicker.getValue() * 1000;
						
						mEditor.putString(SETTING_ANSWERED_CALL_THRESHOLD, Integer.toString(th));
						mEditor.commit();
						mAlertDialog.dismiss();
						updateCallDurationTextView();
					}
				})
				.setNegativeButton(R.string.dialog_call_duration_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						mAlertDialog.dismiss();
					}
				});

				mAlertDialog = alertDialogBuilder.create();
				mAlertDialog.show();
				
				NumberPicker mThresholdPicker = (NumberPicker) mAlertDialog.findViewById(R.id.numberPickerCallDuratrion);
				mThresholdPicker.setMinValue(0);
				mThresholdPicker.setMaxValue(60);
				mThresholdPicker.setWrapSelectorWheel(false);
				
				String durationTh = mSettings.getString(SETTING_ANSWERED_CALL_THRESHOLD, DEFAULT_CALL_DURATION_THRESHOLD);
				int th = Integer.parseInt(durationTh) / 1000;
				mThresholdPicker.setValue(th);
			}
		});


		//==================================================
		// Auto Calls Deletion Spinner

		Spinner callDeleteSpinner = (Spinner) findViewById(R.id.spinnerRemoveCalls);
		String callDeletionType = mSettings.getString(SETTING_CALL_DELETION_TYPE, "NA");
		index = Arrays.asList(mCallDeletionArray).indexOf(callDeletionType);
		if (index != -1) {
			callDeleteSpinner.setSelection(index);
		} else {
			callDeleteSpinner.setSelection(0);
		}

		callDeleteSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String callDeletion = mCallDeletionArray[pos];

				// Saving user's preference for call auto deletion
				SharedPreferences.Editor editor = mSettings.edit();
				editor.putString(SETTING_CALL_DELETION_TYPE, callDeletion);
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//==================================================
		// Auto Outgoing Calls Deletion Spinner

		Spinner outgoingCallDeleteSpinner = (Spinner) findViewById(R.id.spinnerOutgoingCalls);
		String outgoingCallDeletionType = mSettings.getString(SETTING_OUTGOING_CALL_DELETION_TYPE, "NA");
		index = Arrays.asList(mOutgoingCallDeletionArray).indexOf(outgoingCallDeletionType);
		if (index != -1) {
			outgoingCallDeleteSpinner.setSelection(index);
		} else {
			outgoingCallDeleteSpinner.setSelection(0);
		}

		outgoingCallDeleteSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String outgoingCallDeletion =  mOutgoingCallDeletionArray[pos];

				// Saving user's preference for call auto deletion
				SharedPreferences.Editor editor = mSettings.edit();
				editor.putString(SETTING_OUTGOING_CALL_DELETION_TYPE, outgoingCallDeletion);
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		//==================================================
		// Unanswered Calls CheckBox

		Spinner unansweredCallsSpinner = (Spinner) findViewById(R.id.spinnerMissedCalls);
		String unansweredCallsType = mSettings.getString(SETTING_UNANSWERED_CALLS, "NA");
		index = Arrays.asList(mUnansweredCallsArray).indexOf(unansweredCallsType);
		if (index != -1) {
			unansweredCallsSpinner.setSelection(index);
		} else {
			unansweredCallsSpinner.setSelection(0);
		}

		unansweredCallsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				String unansweredCalls =  mUnansweredCallsArray[pos];

				// Saving user's preference for unanswered calls
				SharedPreferences.Editor editor = mSettings.edit();
				editor.putString(SETTING_UNANSWERED_CALLS, unansweredCalls);
				editor.commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void updateCallDurationTextView() {
		String durationTh = mSettings.getString(SETTING_ANSWERED_CALL_THRESHOLD, DEFAULT_CALL_DURATION_THRESHOLD);
		int th = Integer.parseInt(durationTh) / 1000;
		TextView tvCallDuration = (TextView) findViewById(R.id.textViewCallDurationThreshold);
		tvCallDuration.setText(getResources().getString(R.string.pref_call_duration_threshold) + " " +
				Integer.toString(th) + " " + getResources().getString(R.string.pref_call_duration_threshold_seconds));
	}
}
