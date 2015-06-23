package com.jellyfish.illbeback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jellyfish.illbeback.data.CallItem;
import com.jellyfish.illbeback.data.CallsDataSource;
import com.jellyfish.illbeback.widget.CallsAppWidgetProvider;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;


public class Main extends Activity {

	public static final String WIDGET_LAUNCH_APP_ACTION = "WIDGET_LAUNCH_APP_ACTION";
	public static final String CONTACT_ITEM_IN_LIST_ACTION = "CONTACT_ITEM_IN_LIST_ACTION";
	public static final String WIDGET_ITEM_SELECTED_POSITION = "WIDGET_ITEM_POSITION";

	private static final int MENU_DELETE_ID 				= 1001;
	private static final int MENU_REQUEST_CODE_PICK_CONTACT = 1002;

	private static final int[] mCallMangeOffsetsInDays = {0, 1, 7, -1};
	private static int mCallManageOffsetInDays = mCallMangeOffsetsInDays[0];

	private int mCurrentCallId;
	private CallItem mCall;
	private CallsDataSource mDataSource;
	private List<CallItem> mCallsList;
	private ListView mCallsListView;
	private SharedPreferences mAppSettings;
	private int mIntentPosition;
	private AlertDialog mAlertDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mAppSettings = getSharedPreferences(PreferencesActivity.APP_SETTINGS, MODE_PRIVATE);

		mDataSource = new CallsDataSource(this);
		mDataSource.open();

		refreshDisplay();

		// Handling intent data
		Intent intent = this.getIntent();
		if(intent.getAction().equals(CONTACT_ITEM_IN_LIST_ACTION)) {
			mIntentPosition = intent.getIntExtra(WIDGET_ITEM_SELECTED_POSITION, -1);
			if (mIntentPosition != -1) {
				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Main.this);				
				alertDialogBuilder.setTitle(R.string.app_name);
				alertDialogBuilder.setIcon(R.drawable.ic_launcher);

				LayoutInflater inflater = getLayoutInflater();
				View dialogView = (View) inflater.inflate(R.layout.widget_item_pressed_dialog, null);
				alertDialogBuilder.setView(dialogView);

				alertDialogBuilder
				.setCancelable(true)
				.setNegativeButton(R.string.widget_item_action_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						mAlertDialog.dismiss();
						finish();
					}
				});

				mAlertDialog = alertDialogBuilder.create();
				mAlertDialog.show();
				
				// Updating the contact text view
				//-----------------------------------------
				TextView numberTextView = (TextView) dialogView.findViewById(R.id.itemDialogContactNumbertextView);
				ImageView photo = (ImageView) dialogView.findViewById(R.id.itemDialogContactImageView);
				Bitmap contactBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_contact_image);

				CallItem call = mCallsList.get(mIntentPosition);
				String contactID = call.getContactID();
				if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
					// If the number is a contact --> Show contact name
					numberTextView.setText(call.getName());

					if (call.getContactUri() != null) {
						Uri photoUri = Uri.parse(call.getContactUri());
						try {
							contactBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					numberTextView.setText(call.getNumber());
				}

				RoundedImageView roundImg = new RoundedImageView(contactBitmap);
				photo.setImageDrawable(roundImg);
				
				// Call action
				//-----------------------------------------
				ImageView callBtn = (ImageView) dialogView.findViewById(R.id.itemDialogImageViewCall);
				callBtn.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						callHandler(mIntentPosition);
						mAlertDialog.dismiss();
						
					}
				});
				
				// Message action
				//-----------------------------------------
				ImageView msgBtn = (ImageView) dialogView.findViewById(R.id.itemDialogImageViewMsg);
				msgBtn.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						messageHandler(mIntentPosition);
						mAlertDialog.dismiss();
					}
				});
				
				// Delete action
				//-----------------------------------------
				ImageView deleteBtn = (ImageView) dialogView.findViewById(R.id.itemDialogImageViewDelete);
				deleteBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						CallItem call = mCallsList.get(mIntentPosition);
						mDataSource.removeCall(call);
						refreshDisplay();
						mAlertDialog.dismiss();
					}
				});
			}
		}
	}

	private void refreshDisplay() {
		mCallsList = mDataSource.findAllCalls();
		mCallsListView = (ListView) findViewById(R.id.callslistview);
		ArrayAdapter<CallItem> adapter = new CallsListAdapter(this, R.layout.calls_list_adapter, R.id.textViewCallNumber, mCallsList);
		mCallsListView.setAdapter(adapter);
		mCallsListView.setEmptyView(findViewById(R.id.textViewCallsListEmpty));

		registerForContextMenu(mCallsListView);

		UpdateWidget();
	}

	public void callHandler(int position) {
		mCall = mCallsList.get(position);
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + mCall.getNumber()));
		startActivity(callIntent);		
		HandleCallDeletion();
	}

	public void messageHandler(int position) {
		mCall = mCallsList.get(position);
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		String stringData = "sms:" + mCall.getNumber();
		sendIntent.setData(Uri.parse(stringData));	
		startActivity(sendIntent);
		HandleCallDeletion();
	}

	private void HandleCallDeletion() {
		String callDeletionType = mAppSettings.getString(PreferencesActivity.SETTING_CALL_DELETION_TYPE, PreferencesActivity.mCallDeletionArray[0]);

		if (callDeletionType.equals(PreferencesActivity.mCallDeletionArray[1])) {

			// Displaying alert dialog with the contact details before deleting
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setIcon(R.drawable.ic_launcher);
			alertDialogBuilder.setTitle(R.string.app_name);
			alertDialogBuilder.setMessage(R.string.dialog_call_delete_msg);

			LayoutInflater inflater = getLayoutInflater();
			View alertView = inflater.inflate(R.layout.call_dialog, null);
			alertDialogBuilder.setView(alertView);

			alertDialogBuilder
			.setCancelable(true)
			.setPositiveButton(R.string.dialog_remove_call, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Removing the call from the list
					removeCallFromList();
				}
			})
			.setNegativeButton(R.string.dialog_remove_call_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});

			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();

			// Adding contact details to the alert dialog
			TextView numberTextView = (TextView) alertDialog.findViewById(R.id.contactNumbertextView);
			ImageView photo = (ImageView) alertDialog.findViewById(R.id.contactImageView);
			if (!mCall.getContactID().equals(CallItem.CONTACT_ID_UNKOWN)) {
				// If the number is a contact --> Show contact name
				numberTextView.setText(mCall.getName());

				String uriString = mCall.getContactUri();
				if (uriString != null) {
					Uri photoUri = Uri.parse(uriString);
					photo.setImageURI(photoUri);	
				}
			}
			else {
				// If the number is NOT a contact --> Show call number
				numberTextView.setText(mCall.getNumber());
			}
		}
		else if(callDeletionType.equals(PreferencesActivity.mCallDeletionArray[0])) {
			// Removing the call from the list
			removeCallFromList();
		}
	}

	private void removeCallFromList() {
		mDataSource.open();
		mDataSource.removeCall(mCall);
		refreshDisplay();
	}

	private void UpdateWidget() {
		Intent intent = new Intent(this,CallsAppWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), CallsAppWidgetProvider.class));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(intent);
	}

	private void manageCallsList() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = getLayoutInflater();
		View alertView = inflater.inflate(R.layout.calls_manage_layout, null);
		alertDialogBuilder.setView(alertView);

		alertDialogBuilder.setIcon(R.drawable.ic_launcher);
		alertDialogBuilder.setTitle("Remove calls");
		alertDialogBuilder
		.setCancelable(true)
		.setPositiveButton("Remove",new DialogInterface.OnClickListener() {
			@SuppressLint("SimpleDateFormat")
			public void onClick(DialogInterface dialog,int id) {
				mDataSource.open();

				if(mCallManageOffsetInDays >= 0)
				{
					String endDate = Utils.getDateInThePast(mCallManageOffsetInDays);
					mDataSource.DeleteCallsByDates(endDate);
				}
				else
				{
					AlertDialog.Builder deleteAlertDialogBuilder = new AlertDialog.Builder(Main.this);
					deleteAlertDialogBuilder.setIcon(R.drawable.ic_launcher);
					deleteAlertDialogBuilder.setTitle("Warning");
					deleteAlertDialogBuilder
					.setMessage("You are about to remove all calls from your list.\nAre you sure?")
					.setCancelable(true)
					.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							mDataSource.open();
							mDataSource.removeAllCalls();
							refreshDisplay();
						}
					})
					.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							dialog.cancel();
						}
					});

					AlertDialog deleteAlertDialog = deleteAlertDialogBuilder.create();
					deleteAlertDialog.show();
				}
				refreshDisplay();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();


		Spinner callManageSpinner = (Spinner) alertDialog.findViewById(R.id.spinnerCallManage);
		callManageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				mCallManageOffsetInDays = mCallMangeOffsetsInDays[pos];
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	private void addNewCallManually() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Main.this);
		alertDialogBuilder.setTitle(R.string.app_name);
		alertDialogBuilder.setIcon(R.drawable.ic_launcher);

		LayoutInflater inflater = getLayoutInflater();
		View dialogView = (View) inflater.inflate(R.layout.manual_add_call_dialog, null);
		alertDialogBuilder.setView(dialogView);

		alertDialogBuilder
				.setCancelable(true)
				.setNegativeButton(R.string.widget_item_action_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						mAlertDialog.dismiss();
						finish();
					}
				});

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.show();

		// Contacts Action
		//-----------------------------------------
		ImageView contactsBtn = (ImageView) dialogView.findViewById(R.id.itemDialogAddManContactsImg);
		contactsBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
				startActivityForResult(intent, MENU_REQUEST_CODE_PICK_CONTACT);
				mAlertDialog.dismiss();

			}
		});

		// Edit Action
		//-----------------------------------------
		ImageView editBtn = (ImageView) dialogView.findViewById(R.id.itemDialogAddManEditImg);
		editBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mAlertDialog.dismiss();
				addNumberByEdit();
			}
		});
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (MENU_REQUEST_CODE_PICK_CONTACT) :
			if (resultCode == Activity.RESULT_OK) {
				Uri contactUri = data.getData();
				selectContactNumber(contactUri);
			}
		break;
		}
	}

	private void selectContactNumber(Uri contactUri) {
		// Get the contact id from the Uri
		String id = contactUri.getLastPathSegment();

		// Query for phone numbers for the selected contact id
		Cursor  c = getContentResolver().query(
				Phone.CONTENT_URI, null,
				Phone.CONTACT_ID + "=?",
				new String[]{id}, null);

		int phoneIdx = c.getColumnIndex(Phone.NUMBER);
		int phoneType = c.getColumnIndex(Phone.TYPE);

		if(c.getCount() > 1) {
			// Contact has multiple phone numbers
			final CharSequence[] numbers = new CharSequence[c.getCount()];
			int i=0;
			if(c.moveToFirst()) {
				while(!c.isAfterLast()) {
					// For each phone number, add it to the numbers array
					String type = (String) Phone.getTypeLabel(this.getResources(), c.getInt(phoneType), "");
					// Insert a type string in front of the number
					String number = type + ": " + c.getString(phoneIdx);
					numbers[i++] = number;
					c.moveToNext();
				}

				// Build and show a simple dialog that allows the user to select a number
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle(R.string.dialog_title_contact_multiple_number);
				builder.setItems(numbers, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int item) {
						String number = (String) numbers[item];
						int index = number.indexOf(":");
						number = number.substring(index + 2);
						// do something with the selected number
						addSelectedContactToList(number);
					}
				});
				AlertDialog alert = builder.create();
				alert.setOwnerActivity(this);
				alert.show();

			}
		} else if(c.getCount() == 1) {
			// contact has a single phone number, so there's no need to display a second dialog
			if(c.moveToFirst()) {
				addSelectedContactToList(c.getString(phoneIdx));
			}
		}
	}

	private void addNumberByEdit() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.app_name);
		alertDialogBuilder.setIcon(R.drawable.ic_launcher);

		LayoutInflater inflater = getLayoutInflater();
		View alertView = inflater.inflate(R.layout.manual_add_call_edit, null);
		alertDialogBuilder.setView(alertView);

		alertDialogBuilder
				.setCancelable(true)
				.setPositiveButton(R.string.action_add_call, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						EditText editNumber = (EditText)mAlertDialog.findViewById(R.id.editTextPhoneNumEdit);
						addSelectedContactToList(editNumber.getText().toString());
						mAlertDialog.dismiss();
					}
				})
				.setNegativeButton(R.string.dialog_remove_call_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						mAlertDialog.dismiss();
					}
				});

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mAlertDialog.dismiss();
			}
		});

		mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		mAlertDialog.show();
	}

	private void addSelectedContactToList(String number) {
		ArrayList<CallItem> intentCallsList = new ArrayList<CallItem>();
		CallItem call = ContactUtils.fillCallDetails(this, number, 0);
		intentCallsList.add(call);

		ArrayList<Integer> intentFireDialogList = new ArrayList<Integer>();
		intentFireDialogList.add(0);
		ArrayList<Integer> intentCallWaitingList = new ArrayList<Integer>();
		intentCallWaitingList.add(0);
		ArrayList<Integer> intentIsOutgoingCallList = new ArrayList<Integer>();
		intentIsOutgoingCallList.add(0);

		Intent dialogIntent = new Intent(this, CallDialogActivity.class);
		dialogIntent.putParcelableArrayListExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_CALLS_LIST, intentCallsList);
		dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_IS_FIRE_LIST, intentFireDialogList);
		dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_CALL_WAITING_LIST, intentCallWaitingList);
		dialogIntent.putExtra(CallDialogActivity.CALL_DIALOG_ACTIVITY_IS_OUTGOING_CALL_LIST, intentIsOutgoingCallList);
		startActivity(dialogIntent);
	}

	@Override
	protected void onResume() {
		refreshDisplay();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		mCurrentCallId = (int) info.id;
		menu.add(0, MENU_DELETE_ID, 0, "Delete");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_DELETE_ID) {
			CallItem call = mCallsList.get(mCurrentCallId);
			mDataSource.removeCall(call);
			refreshDisplay();
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			Intent intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
		}
		else if (item.getItemId() == R.id.action_remove_calls) {
			manageCallsList();
		}
		else if (item.getItemId() == R.id.action_add_call) {
			addNewCallManually();
		}
		else if (item.getItemId() == R.id.action_about) {
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
		}

		return super.onOptionsItemSelected(item);
	}
}
