package com.jellyfish.illbeback;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.widget.ImageView;

import com.jellyfish.illbeback.data.CallItem;

public class ContactUtils {

	public static CallItem fillCallDetails(Context context, String number, long callDuration) {
		CallItem call = new CallItem();

		String contactID;
		String contactName;
		Uri contactPhotoUri;

		call.setNumber(number);
		call.setDate(Utils.getCallDate());		
		call.setHour(Utils.getCallHour());
		call.setDuration(callDuration);
		call.setRepetitions(1);

		contactID  = contactExists(context, number);
		call.setContactID(contactID);

		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			// Trying to get the contact name
			contactName = getContactName(context, number);
			if (contactName != null) {
				call.setName(contactName);
			}

			// Checking if the user has a contact image
			contactPhotoUri = getContactPhotoUri(context, contactID);
			if (contactPhotoUri != null) {
				ImageView contactimage = new ImageView(context);
				contactimage.setImageURI(contactPhotoUri);
				if (contactimage.getDrawable() != null) {
					call.setContactUri(contactPhotoUri.toString());
				}
			}
		} else {
			call.setName(number);
			call.setContactUri(null);
		}

		return call;
	}

	private static String contactExists(Context context, String number) {
		Uri lookupUri = Uri.withAppendedPath( PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		String[] phoneNumberProjection = { PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME };
		String ID = CallItem.CONTACT_ID_UNKOWN;

		Cursor cur = context.getContentResolver().query(lookupUri, phoneNumberProjection, null, null, null);		
		if (cur != null && cur.moveToFirst())
		{
			// Number was found
			ID = cur.getString(cur.getColumnIndex(PhoneLookup._ID));
		}

		cur.close();
		return ID;
	}

	private static String getContactName(Context context, String number) {
		Uri lookupUri = Uri.withAppendedPath( PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		String[] mPhoneNumberProjection = { PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME };
		Cursor cur = context.getContentResolver().query(lookupUri, mPhoneNumberProjection, null, null, null);		
		if (cur != null && cur.moveToFirst())
		{
			// Number was found
			return cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		} 
		else if (cur != null) {
			cur.close();
		}

		return null;
	}

	private static Uri getContactPhotoUri(Context context, String id) {
		Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
		return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
	}

	public static String getContactNumber(Context context, Uri uriContact) {

		String contactNumber = null;
		String contactID = null;

		// getting contacts ID
		Cursor cursorID = context.getContentResolver().query(uriContact, new String[]{ContactsContract.Contacts._ID},
				null, null, null);

		if (cursorID.moveToFirst()) {

			contactID = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
		}

		cursorID.close();

		// Using the contact ID now we will get contact phone number
		Cursor cursorPhone = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

				ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
						ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
						ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
						new String[]{contactID},
						null);

		if (cursorPhone.moveToFirst()) {
			contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		}

		cursorPhone.close();
		return contactNumber;
	}
}
