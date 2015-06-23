package com.jellyfish.illbeback.data;

import android.os.Parcel;
import android.os.Parcelable;

public class CallItem implements Parcelable {

	public static final long CALL_ID_UNKOWN 		= -1;
	public static final String CONTACT_ID_UNKOWN	= "CONTACT_ID_UNKOWN";

	public static final String FIELD_ID 			= "ID";
	public static final String FIELD_DATE 			= "date";
	public static final String FIELD_HOUR 			= "hour";
	public static final String FIELD_NAME			= "name";
	public static final String FIELD_NUMBER 		= "number";
	public static final String FIELD_DURATION 		= "duration";
	public static final String FIELD_CONTACT_ID 	= "contactID";
	public static final String FIELD_CONTACT_URI	= "uri";
	public static final String FIELD_REPETITIONS 	= "repetitions";

	private long id;
	private String date;
	private String hour;
	private String name;
	private String number;
	private long duration;
	private String contactID;
	private String contactUri;
	private int repetitions;
	
	public CallItem() {
	}

	public CallItem(long id, String date, String hour, String name,
			String number, long duration, String contactID, String contactUri,
			int repetitions) {
		this.id = id;
		this.date = date;
		this.hour = hour;
		this.name = name;
		this.number = number;
		this.duration = duration;
		this.contactID = contactID;
		this.contactUri = contactUri;
		this.repetitions = repetitions;
	}

	public CallItem(Parcel in) {
		this.id = in.readLong();
		this.date = in.readString();
		this.hour = in.readString();
		this.name = in.readString();
		this.number = in.readString();
		this.duration = in.readLong();
		this.contactID = in.readString();
		this.contactUri = in.readString();
		this.repetitions = in.readInt();
	}

	public static final Parcelable.Creator<CallItem> CREATOR = new Parcelable.Creator<CallItem>() {
		public CallItem createFromParcel(Parcel in) {
			return new CallItem(in);
		}

		public CallItem[] newArray(int size) {
			return new CallItem[size];
		}
	};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(this.date);
		dest.writeString(this.hour);
		dest.writeString(this.name);
		dest.writeString(this.number);
		dest.writeLong(this.duration);
		dest.writeString(this.contactID);
		dest.writeString(this.contactUri);
		dest.writeInt(this.repetitions);
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getHour() {
		return hour;
	}
	public void setHour(String hour) {
		this.hour = hour;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public String getContactUri() {
		return contactUri;
	}
	public void setContactUri(String contactUri) {
		this.contactUri = contactUri;
	}

	public String getContactID() {
		return contactID;
	}
	public void setContactID(String contactID) {
		this.contactID = contactID;
	}

	public int getRepetitions() {
		return repetitions;
	}
	public void setRepetitions(int repetitions) {
		this.repetitions = repetitions;
	}
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
}
