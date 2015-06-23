package com.jellyfish.illbeback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jellyfish.illbeback.data.CallItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;


public class CallListDialogAdapter extends ArrayAdapter<CallItem> {
	
	private Context mContext;
	private View mRow;
	private List<CallItem> mCallsList;
	private ArrayList<Integer> mCallWaiting;
	private Bitmap mBitmap;
	private RoundedImageView mRoundedImage;
	
	public CallListDialogAdapter(Context context, int resource, int textViewResourceId, List<CallItem> calls, List<Integer> callWaiting) {
		super(context, resource, textViewResourceId, calls);
		this.mContext = context;
		this.mCallsList = new ArrayList<CallItem>();
		this.mCallsList.addAll(calls);
		this.mCallWaiting = new ArrayList<Integer>();
		this.mCallWaiting.addAll(callWaiting);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRow = inflater.inflate(R.layout.call_dialog_list_adapter, parent, false);
		
		// Contact image
		//----------------------
		ImageView iv = (ImageView) mRow.findViewById(R.id.imageViewDialogContactImage);
		mBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_contact_image);

		String uriString = mCallsList.get(position).getContactUri();
		if (uriString != null) {
			Uri imageUri = Uri.parse(uriString);
			//iv.setImageURI(imageUri);
			
			try {
				mBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageUri);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mRoundedImage = new RoundedImageView(mBitmap);
		iv.setImageDrawable(mRoundedImage);
		
		// Call Number
		//----------------------
		TextView numberTextView = (TextView) mRow.findViewById(R.id.textViewDialogCallNumber);
		String contactID = mCallsList.get(position).getContactID();
		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			// If the number is a contact --> Show contact name
			numberTextView.setText(mCallsList.get(position).getName());
		}
		else {
			// If the number is NOT a contact --> Show call number
			numberTextView.setText(mCallsList.get(position).getNumber());
		}
		
		// Call Waiting
		//----------------------
		TextView callWaitingTextView = (TextView) mRow.findViewById(R.id.textViewDialogCallWaiting);
		if (mCallWaiting.get(position) == 0) {
			callWaitingTextView.setVisibility(View.GONE);
		}
		
		// Check Box
		//----------------------
		CheckBox addCallCb = (CheckBox) mRow.findViewById(R.id.checkBoxDialogCall);
		addCallCb.setTag(position);
		addCallCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				int position = (Integer)buttonView.getTag();
				if (isChecked) {
					CallDialogActivity.addItemPositionToCallToAdd(position);
				} else {
					CallDialogActivity.removeItemPositionToCallToAdd(position);
				}
			}
		});
		
		return mRow;
	}

}
