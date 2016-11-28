package cn.flyingwings.robot.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StringUtil {
	/**
	 * test if the string is null or empty.
	 * @param string
	 * 		the string to be test
	 * @return
	 * 		true if the string is null or empty, false otherwise
	 */
	public static boolean isNullOrEmpty(String string){
		return null == string || string.length() <= 0;
	}
	
	/**
	 * get current time string. format: yyyy.MM.dd HH:mm 
	 * @return
	 * 		the formatted time string
	 */
	public static String getCurrentTimeString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US);
		return dateFormat.format(new Date());
	}
}