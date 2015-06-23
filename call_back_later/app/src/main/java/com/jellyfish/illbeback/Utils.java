package com.jellyfish.illbeback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;

@SuppressLint("SimpleDateFormat")
public class Utils {

	public static String getCallDate() {
		
		Locale locale = new Locale("en_US");
		Locale.setDefault(locale);
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		String date = formatter.format(new Date());
		return date;
	}
	
	public static String getCallHour() {

		Locale locale = new Locale("en_US");
		Locale.setDefault(locale);
		String pattern = "HH:mm:ss";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		String date = formatter.format(new Date());
		return date;
	}
	
	public static String getDateInThePast(int offsetInDays) {
		
		Locale locale = new Locale("en_US");
		Locale.setDefault(locale);
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern);
		
		Calendar cal = Calendar.getInstance();
		int offset = -1 * offsetInDays;
		cal.add(Calendar.DATE, offset);
		
		String date = formatter.format(cal.getTime());
		return date;
	}
	
	public static int boolToInt(boolean b) {
	    return b ? 1 : 0;
	}
}
