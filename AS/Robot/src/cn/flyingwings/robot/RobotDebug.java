package cn.flyingwings.robot;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.util.Log;

public class RobotDebug {
	
	public static void out(String s) {
		final Throwable t = new Throwable();
		final StackTraceElement ste = t.getStackTrace()[1];
		String cName = ste.getClassName();
		String tag = cName.substring(cName.lastIndexOf('.')+1);
		Log.d(tag,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ s);
	}
	
	public static void d(String TAG, String msg) {
		Log.d(TAG, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ msg);
	}

	public static void e(String TAG, String msg) {
		Log.e(TAG,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " " + msg);
	}
	
	public static void i(String TAG, String msg) {
		Log.i(TAG,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " " + msg);
	}
}
