package cn.flyingwings.robot.motion;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.util.Log;

public class MotionLog {

		public static boolean StopLog = false;
		
		public static boolean StopDebugLog = true;
		
		public static boolean StopInfoLog = true;
		
		public static boolean StopVerboseLog = true;
		
		public static boolean StopWarningLog = true;
		
		public static boolean StopErrorLog = false;
		

		public static void e (String TAGS, String msg)
		{
			if( (MotionLog.StopLog == false) && (MotionLog.StopErrorLog == false)) 
				Log.e(TAGS,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ msg);
			
		}
		
		
		public static void i (String TAGS, String msg)
		{
			if((MotionLog.StopLog == false) && (MotionLog.StopInfoLog == false) )
				Log.i(TAGS,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ msg);
			
		}
		
		
		public static void w (String TAGS, String msg)
		{
			if((MotionLog.StopLog == false) && (MotionLog.StopWarningLog == false))
				Log.w(TAGS, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+msg);
			
		}
		
		
		public static void d (String TAGS, String msg)
		{
			if((MotionLog.StopLog == false) && (MotionLog.StopDebugLog == false))
				Log.d(TAGS,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ msg);
			
		}
		
		
		public static void v (String TAGS, String msg)
		{
			
			if((MotionLog.StopLog == false) && (MotionLog.StopVerboseLog == false))
				Log.v(TAGS,(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")).format(new Date()) + " "+ msg);
		}
	}