package com.jellyfish.illbeback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jellyfish.illbeback.data.CallItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;

public class CallsListAdapter extends ArrayAdapter<CallItem> {
	
	private static final int BUTTON_CLICK_BACKGROUND = 0xFF57BEFF;
	
	private Context context;
	private View row;
	private List<CallItem> callsList;
	private Bitmap mBitmap;
	private RoundedImageView mRoundedImage;
	
	public CallsListAdapter(Context context, int resource, int textViewResourceId, List<CallItem> calls) {
		super(context, resource, textViewResourceId, calls);
		this.context = context;
		this.callsList = new ArrayList<CallItem>();
		this.callsList.addAll(calls);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		row = inflater.inflate(R.layout.calls_list_adapter, parent, false);
		
		// Contact image
		//----------------------
		ImageView iv = (ImageView) row.findViewById(R.id.imageViewContactImage);
		mBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.default_contact_image);

		String uriString = callsList.get(position).getContactUri();
		if (uriString != null) {
			Uri imageUri = Uri.parse(uriString);
			//iv.setImageURI(imageUri);
			
			try {
				mBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
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
		TextView numberTextView = (TextView) row.findViewById(R.id.textViewCallNumber);
		String contactID = callsList.get(position).getContactID();
		if (!contactID.equals(CallItem.CONTACT_ID_UNKOWN)) {
			// If the number is a contact --> Show contact name
			numberTextView.setText(callsList.get(position).getName());
		}
		else {
			// If the number is NOT a contact --> Show call number
			numberTextView.setText(callsList.get(position).getNumber());
		}
		
		// Call Repetitions
		//----------------------
		TextView repetitionsTextView = (TextView) row.findViewById(R.id.textViewCallRepetition);
		int repetitions = callsList.get(position).getRepetitions();
		if (repetitions == 1) {
			repetitionsTextView.setText("");
		}
		else {
			repetitionsTextView.setText("(" + repetitions + ")");
		}
		
		// Call Date
		//----------------------
		TextView dateTextView = (TextView) row.findViewById(R.id.textViewCallDate);
		dateTextView.setText(callsList.get(position).getDate());
		
		// Call Hour
		//----------------------
		TextView hourTextView = (TextView) row.findViewById(R.id.textViewCallHour);
		hourTextView.setText(callsList.get(position).getHour());

		ImageView callImageView = (ImageView) row.findViewById(R.id.imageViewCall);
		callImageView.setTag(position);
		callImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				v.setBackgroundColor(BUTTON_CLICK_BACKGROUND);
				int position = (Integer)v.getTag();
				((Main)context).callHandler(position);
			}
		});
		
		ImageView msgImageView = (ImageView) row.findViewById(R.id.imageViewMsg);
		msgImageView.setTag(position);
		msgImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				v.setBackgroundColor(BUTTON_CLICK_BACKGROUND);
				int position = (Integer)v.getTag();
				((Main)context).messageHandler(position);
			}
		});
		
		
		return row;
	}

}
