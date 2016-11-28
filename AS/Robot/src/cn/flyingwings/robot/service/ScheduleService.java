package cn.flyingwings.robot.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.smartdeviceservice.SmartDevLog;
import cn.flyingwings.robot.smartdeviceservice.SmartdevTools;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 定时任务服务<br>
 * 按照每日的时分和每周的重复日设置定时任务并进行定时任务调度。
 * @author gd.zhang
 *
 */
public class ScheduleService extends RobotService {
	public static final String NAME = "schedule";
	
	/* 定时任务重复天数：周日 */
	public static final int SUNDAY 		= 0x01;
	/* 定时任务重复天数：周一 */
	public static final int MONDAY 		= 0x02;
	/* 定时任务重复天数：周二 */
	public static final int TUESDAY 	= 0x04;
	/* 定时任务重复天数：周三 */
	public static final int WEDNESDAY 	= 0x08;
	/* 定时任务重复天数：周四 */
	public static final int THURSAY 	= 0x10;
	/* 定时任务重复天数：周五 */
	public static final int FRIDAY 		= 0x20;
	/* 定时任务重复天数：周六 */
	public static final int SATURDAY 	= 0x40;
	
	private static final String TAG = "ScheduleService";
	private static final String ACTION_SCHEDULE = "cn.flyingwings.robot.action_schedule";
	
	private static final String DATABASE_NAME = "schedule.db";
	
	private Context mContext;
	private AlarmManager mAlarm;
	private PendingIntent pendingIntent = null;
	
	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public void start() {
		Robot robot = Robot.getInstance();
		mContext = robot.getContext();
		mAlarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		/* 注册定时任务的广播接收器 */
		IntentFilter intentFilter = new IntentFilter(ACTION_SCHEDULE);
		mContext.registerReceiver(receiver, intentFilter);
		
		/* 读取数据库，添加定时器 */
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			/* 判断表是否存在，如果不存在则新建一个 */
			cursor = db.rawQuery("select count(*) from sqlite_master where `type`='table' and `name`='schedule' ", null);
			cursor.moveToFirst();
			int ret = cursor.getInt(0);
			cursor.close();
			if (ret == 0) {
				StringBuffer sb = new StringBuffer();
				sb.append("create table `schedule` (");
				sb.append("`tid` integer not null primary key autoincrement,");
				sb.append("`name` varchar(255) not null,");
				sb.append("`action` varchar(2048) not null,");
				sb.append("`time` varchar(6) not null,");
				sb.append("`repeat` int(8) not null default 0,");
				sb.append("`enable` tinyint not null default 1,");
				sb.append("`next` long default 0)");
				db.execSQL(sb.toString());
			}
			
			/* 设置已经过期的不重复的闹铃 */
			cursor = db.rawQuery("select `tid`,`time`,`repeat`,`enable` from `schedule` where `enable`=1 and `repeat`=0", null);
			if(cursor.getCount() > 0)
			{
				cursor.moveToFirst();
				do{
					String time = cursor.getString(1);
					String[] times = time.split(" ");
					if(times.length == 2){
						String time_YMD = times[0];
						String time_HM  = times[1];
						long time_now = System.currentTimeMillis();
						String[] time_YMDs = time_YMD.split(":");
						String[] time_HMs = time_HM.split(":");
						int year  = Integer.valueOf(time_YMDs[0]);
						int month = Integer.valueOf(time_YMDs[1]);
						int day = Integer.valueOf(time_YMDs[2]);
						int hour = Integer.valueOf(time_HMs[0]);
						int minute = Integer.valueOf(time_HMs[1]);
						Calendar cal = Calendar.getInstance();
						cal.set(year, month, day, hour, minute, 0);
						if(time_now > cal.getTimeInMillis()){
							db.execSQL("update `schedule` set `enable`=0"+" where `tid`="+cursor.getInt(0));
						}
					}
				}while(cursor.moveToNext());
			}
			
			/* 查询数据库，逐个更新定时任务 */
			cursor = db.rawQuery("select `tid`,`time`,`repeat` from `schedule` where `enable`=1 and `repeat`>=0", null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					/* 计算下次触发时间 */
					final long triggerAt = getNextTime(cursor.getString(1), cursor.getInt(2));
					if (triggerAt > 0)
						db.execSQL("update `schedule` set `next`="+triggerAt+" where `tid`="+cursor.getInt(0));
				} while (cursor.moveToNext());
			}
			setNextAlarm(db);
		} catch (Exception e) {
			Log.d(TAG, "加载定时任务数据失败", e);
		} finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}

		super.start();
	}
	
	@Override
	public void stop() {
		mContext.unregisterReceiver(receiver);
		mAlarm.cancel(pendingIntent);

		/* 清理持有的对象引用 */
		mContext = null;
		mAlarm = null;
		super.stop();
	}
	
	/**
	 * 添加一个定时任务，默认为启用状态
	 * @param name 定时任务的名称
	 * @param action 定时任务的动作请求 JSON 字符串
	 * @param time 定时任务的每日触发时间，格式为二十四小时的时分字符串，"HH:mm" 
	 * @param repeat 每周的重复日，按照需要的重复日求和，见 {@link #SUNDAY}, {@link #MONDAY},
	 * {@link #TUESDAY}, {@link #WEDNESDAY}, {@link #THURSAY}, {@link #FRIDAY}, {@link #SATURDAY}
	 * @return int tid
	 */
	public synchronized boolean addTask(String name, String action, String time, int repeat) {
		boolean ret = true;
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			final long triggerAt;
			if (repeat > 0) {
				triggerAt = getNextTime(time, repeat);	
			} else {
				triggerAt = 0;
			}
			String sql = String.format(Locale.getDefault(), "insert into `schedule` (`name`,`action`,`time`,`repeat`,`next`) values (\"%s\",\"%s\",\"%s\",%d,%d)",
					name, sql_escape(action), time, repeat, triggerAt);
			db.execSQL(sql);
			mAlarm.cancel(pendingIntent);
			setNextAlarm(db);
		} catch (Exception e) {
			ret = false;
			Log.d(TAG, "添加定时任务失败", e);
		}finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return ret;
	}
	
	
	/**
	 * 添加一个定时任务，默认为启用状态
	 * @param name 定时任务的名称
	 * @param action 定时任务的动作请求 JSON 字符串
	 * @param time 定时任务的每日触发时间，格式为二十四小时的时分字符串，"HH:mm" 
	 * @param repeat 每周的重复日，按照需要的重复日求和，见 {@link #SUNDAY}, {@link #MONDAY},
	 * {@link #TUESDAY}, {@link #WEDNESDAY}, {@link #THURSAY}, {@link #FRIDAY}, {@link #SATURDAY}
	 * @return int tid if tid:-1 failed  
	 */
	public synchronized int addTaskForApp(String name,String action,String time,int repeat)
	{
		String time_need = time;
		int ret = -1;
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			final long triggerAt;
			if (repeat > 0) {
				triggerAt = getNextTime(time_need, repeat);
			}else if (repeat == 0){
				String[] times = time_need.split(":");
				if(times.length == 2)
				{
					int hour   = Integer.valueOf(times[0]);
					int minute = Integer.valueOf(times[1]);
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.HOUR_OF_DAY, hour);
					cal.set(Calendar.MINUTE, minute);
					cal.set(Calendar.SECOND, 0);
					if(System.currentTimeMillis() > cal.getTimeInMillis()){
						cal.set(Calendar.DAY_OF_MONTH,cal.get(Calendar.DAY_OF_MONTH)+1);
					}
					SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd");
					Date  needDate  = new Date(cal.getTimeInMillis());
					String time_YMD = format.format(needDate);
					time_need = String.format("%s %s",time_YMD,time);
					triggerAt = getNextTime(time_need,repeat);
				}
				else
					triggerAt = 0;
			}
			else {
				triggerAt = 0;
			}
			RobotDebug.d(TAG, "in addtask  need_time " + time_need);
			String sql = String.format(Locale.getDefault(), "insert into `schedule` (`name`,`action`,`time`,`repeat`,`next`) values (\"%s\",\"%s\",\"%s\",%d,%d)",
					name, sql_escape(action), time_need, repeat, triggerAt);
			db.execSQL(sql);
			cursor = db.rawQuery("select `tid`,`name`,`action`,`time`,`repeat`,`enable` from `schedule` ", null);
			if ((cursor!= null) && (cursor.getCount() > 0)) {
				if(cursor.moveToLast()){
					ret = cursor.getInt(0);
				}
			}
			cursor.close();
			mAlarm.cancel(pendingIntent);
			setNextAlarm(db);
		} catch (Exception e) {
			ret = -1;
			Log.d(TAG, "添加定时任务失败", e);
			cursor.close();
		}finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return ret;
	}
	/**
	 * 删除一个定时任务
	 * @param tid 定时任务的唯一任务id
	 * @return 是否删除成功，删除过程中出错返回 {@code false}
	 */
	public synchronized boolean delTask(int tid) {
		boolean ret = true;
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			/* 获取当前定时器 */
			cursor = db.rawQuery("select `tid` from `schedule` where `next`>0 and `enable`=1 and `repeat`>=0 order by `next` asc limit 1", null);
			int current = 0;
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				current = cursor.getInt(0);
			}
			cursor.close();
			/* 删除定时任务 */
			db.execSQL("delete from `schedule` where `tid`="+tid);
			/* 如果删除了当前定时器的任务，更新当前定时器 */
			if (tid == current) {
				mAlarm.cancel(pendingIntent);
				setNextAlarm(db);
			}
		} catch (Exception e) {
			ret = false;
			Log.d(TAG, "删除定时任务失败", e);
		}finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return ret;
	}
	
	/**
	 * 设置一个定时任务的启用或禁用状态
	 * @param tid 定时任务的唯一id
	 * @param enable 启用或禁用的状态
	 * @return 是否设置成功，找不到任务id或设置过程中出错，返回 {@code false}
	 */
	@SuppressWarnings("deprecation")
	public synchronized boolean setEnable(int tid, boolean enable) {
		boolean ret = true;
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			cursor = db.rawQuery("select `time`,`repeat`,`enable` from `schedule` where `tid`="+tid, null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				boolean old_en = (cursor.getInt(2) == 1);
				String time = cursor.getString(0);
				/* 判断状态是否有改变 */
				if (enable != old_en) {
					long triggerAt = 0;
					if (enable && cursor.getInt(1) > 0) {
						triggerAt = getNextTime(cursor.getString(0), cursor.getInt(1));
					}
					else if(enable && (cursor.getInt(1) == 0) && (!old_en))
					{
						//已经执行过或者过期的闹铃，需要修改time time 为yyyy:MM:dd HH:mm 
						RobotDebug.d(TAG, "setEnable original time : " + time);
						String[] times = time.split(" ");
						if(times.length == 2){
							String timeYMD = times[0];
							String timeHM  = times[1];
							int split = timeHM.indexOf(':');
							final int hour = Integer.parseInt(timeHM.substring(0, split));
							final int minute = Integer.parseInt(timeHM.substring(split+1, timeHM.length()));
							RobotDebug.d(NAME,"timer hour : " + hour + "minute :" + minute);
							final long now = System.currentTimeMillis();
							Calendar cal =  Calendar.getInstance();
							cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
							cal.set(Calendar.HOUR_OF_DAY,hour);
							cal.set(Calendar.MINUTE,minute);
							cal.set(Calendar.SECOND, 0);
							SimpleDateFormat simpleDateformat2 = new SimpleDateFormat("yyyy:MM:dd HH:mm");
							Date needDate2  = new Date(cal.getTimeInMillis());
							time = simpleDateformat2.format(needDate2);
							RobotDebug.d(TAG, "setEnable before time1 : " + time);
							if( cal.getTimeInMillis() < now){
								cal.add(Calendar.DATE, 1);
							}
							SimpleDateFormat simpleDateformat = new SimpleDateFormat("yyyy:MM:dd HH:mm");
							Date needDate  = new Date(cal.getTimeInMillis());
							time = simpleDateformat.format(needDate);
							RobotDebug.d(TAG, "setEnable after time1 : " + time);
							triggerAt = getNextTime(time,0);
						}
						else{
							RobotDebug.d(NAME,"repeat=0 time unuse: " + time);
						}
					}
					RobotDebug.d(TAG, "setEnable time2 : " + time);
					db.execSQL(String.format(Locale.getDefault(), "update `schedule` set `enable`=%d,`next`=%d,`time`=\"%s\" where `tid`=%d",
							(enable?1:0),triggerAt,time,tid));
					/* 如果有重复天数，更新当前定时器 */
					if (cursor.getInt(1) >= 0) {
						mAlarm.cancel(pendingIntent);
						setNextAlarm(db);
					}
				}
			} else {
				ret = false;
				RobotDebug.d(TAG, "Can not find tid "+tid+" to enable");
			}
		} catch (Exception e) {
			ret = false;
			Log.d(TAG, "切换定时任务失败", e);
		}finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return ret;
	}
	
	/**
	 * 更新一个定时任务的设置
	 * @param tid 定时任务的唯一id
	 * @param name 定时任务的新名称
	 * @param action 定时任务的新动作请求 JSON 字符串
	 * @param time 定时任务的新执行时间
	 * @param repeat 定时任务的新每周重复日
	 * @return 是否更新成功，找不到定时任务id或更新过程中出错则返回 {@code false}
	 */
	public synchronized boolean modifyTask(int tid, String name, String action, String time, int repeat) {
		String need_time = time;
		boolean ret = true;
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			cursor = db.rawQuery("select `enable` from `schedule` where `tid`="+tid, null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				boolean enable = (cursor.getInt(0) == 1);
				long triggerAt = 0;
				if(repeat > 0)
						triggerAt = getNextTime(time,repeat);
				else if(repeat == 0){
						String[] times = time.split(":");
						if(times.length == 2)
						{
							int hour   = Integer.valueOf(times[0]);
							int minute = Integer.valueOf(times[1]);
							Calendar cal = Calendar.getInstance();
							cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
							cal.set(Calendar.HOUR_OF_DAY, hour);
							cal.set(Calendar.MINUTE, minute);
							cal.set(Calendar.SECOND, 0);
							if(System.currentTimeMillis() > cal.getTimeInMillis()){
								cal.add(Calendar.DATE, 1);
							}
							SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd");
							Date  needDate  = new Date(cal.getTimeInMillis());
							String time_YMD = format.format(needDate);
							need_time = String.format("%s %s",time_YMD,time);
							triggerAt = getNextTime(need_time,repeat);
						}
						else
							triggerAt = 0;
				}
				else{
						need_time = String.format("%s %s", SmartdevTools.getCurrentTimeYMD(),time);
						triggerAt = getNextTime(need_time,repeat);
				}
				RobotDebug.d(TAG, "modifyTask  need_time: " + need_time + "  time: " + time);
				String sql = String.format(Locale.getDefault(),
						"update `schedule` set `name`=\"%s\",`action`=\"%s\",`time`=\"%s\",`repeat`=%d,`next`=%d,`enable`=%d where `tid`=%d",
						name, sql_escape(action), need_time, repeat, triggerAt,1,tid);
				db.execSQL(sql);
				/* 如果是开启的需要取消定时器 */
				if (enable) {
					RobotDebug.d(TAG, "modifyTask  enable");
					mAlarm.cancel(pendingIntent);
				}
				//更新定时器
				setNextAlarm(db);
			} else {
				ret = false;
				RobotDebug.d(TAG, "Can not find tid "+tid+" to modify");
			}
		} catch (Exception e) {
			ret = false;
			Log.d(TAG, "更改定时任务失败", e);
		}finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return ret;
	}
	
	/**
	 * 将所有的定时任务信息传给到客户端
	 * @return 是否发送成功，发送过程中出错则返回 {@code false}
	 */
	public JSONArray queryAllTask(int index) {
		int count     = 0;
        int needstart = 0;
        int needend   = 0;
        if(index < 0)
       	  	return null;
        if(index == 0 || index == 1)
        {
       	 	needstart = 0;
       	 	needend = 19;
        }
        else if(index > 1)
        {
        	needstart = (index-1) * 20 ;
       	 	needend = index*20 -1;
        }
		Cursor cursor = null;
		SQLiteDatabase db = null;
		JSONArray reminders = new JSONArray();
		try {
			db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
			/* 查询数据库，逐个处理定时任务 */
			cursor = db.rawQuery("select `tid`,`name`,`action`,`time`,`repeat`,`enable` from `schedule` ", null);
			 if(cursor.moveToLast() && (needstart == 0) )
		     {
		        	   int tid = cursor.getInt(0);// tid
		        	   String name   = cursor.getString(1);// name.
		        	   String action = cursor.getString(2);// action
		        	   RobotDebug.d(NAME, "action : " + action);
		        	   String time = cursor.getString(3);// time
		        	   int repeat = cursor.getInt(4);//repeat
		        	   if(repeat == 0 )
		        	   {
		        		   RobotDebug.d(TAG, "quiry time1 :  " + time);
		        		   String[] times = time.split(" ");
		        		   RobotDebug.d(TAG, "quiry times :  " + times[0] + "   " + times[1]);
		        		   if(times.length == 2)
		        		   {
		        			   time = times[1];
		        		   }
		        		   RobotDebug.d(TAG, "quiry time2 :  " + time);
		        	   }
		        	   RobotDebug.d(TAG, "quiry time2 :  " + time);
		        	   int enable = cursor.getInt(5);// enable
		        	   String enable_temp = "F";
		        	   if(enable == 1)
		        		   enable_temp = "T";
		        	   JSONObject reminder_temp = new JSONObject();
		        	   try{
		        		   reminder_temp.put("id",tid);
		        		   reminder_temp.put("name", name);
		        		   reminder_temp.put("content", name);
		        		   reminder_temp.put("time", time);
		        		   reminder_temp.put("week", repeat);
		        		   reminder_temp.put("enable", enable_temp);
		        		   reminders.put(reminder_temp);
		        	   }catch (JSONException e) {
		        		   RobotDebug.d(TAG, "reminder is not json format e." + e.toString());
		        	   }
		     }
			 while (cursor.moveToPrevious()) {
		    		 if(count < needstart )
		    		 {
		    			 count++;
		    			 continue;
		    		 }
		    		 if(count >= needend)
		    			break;
		    		 int tid = cursor.getInt(0);// tid
		        	 String name   = cursor.getString(1);// name.
		        	 String action = cursor.getString(2);// action
		        	 String time = cursor.getString(3);// time
		        	 RobotDebug.d(NAME, "action : " + action);
		        	 int repeat = cursor.getInt(4);//repeat
		        	 int enable = cursor.getInt(5);// enable
		        	 String enable_temp = "F";
		        	 if(enable == 1)
		        		   enable_temp = "T";
		        	   if(repeat == 0 )
		        	   {
		        		   RobotDebug.d(TAG, "quiry time1 :  " + time);
		        		   String[] times = time.split(" ");
		        		   RobotDebug.d(TAG, "quiry times :  " + times[0] + "   " + times[1]);
		        		   if(times.length == 2)
		        		   {
		        			   time = times[1];
		        		   }
		        		   RobotDebug.d(TAG, "quiry time2 :  " + time);
		        	   }
		        	   RobotDebug.d(TAG, "quiry time2 :  " + time);
		        	 JSONObject reminder_temp = new JSONObject();
		        	 try{
		        		   reminder_temp.put("id",tid);
		        		   reminder_temp.put("name", name);
		        		   reminder_temp.put("content", name);
		        		   reminder_temp.put("time", time);
		        		   reminder_temp.put("week", repeat);
		        		   reminder_temp.put("enable", enable_temp);
		        		   reminders.put(reminder_temp);
		        	 }catch (JSONException e) {
		        		   RobotDebug.d(TAG, "reminder is not json format e." + e.toString());
		        	 }
		        	 count++;
		   }
		   cursor.close();
		   db.close();
		} catch (Exception e) {
			RobotDebug.d(TAG, "查询定时任务失败  e:" + e.toString());
		} finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
			if (db != null && db.isOpen())
				db.close();
		}
		return reminders;
	}
	
	private PendingIntent getPendingIntent(Context c, int tid, String action, String time, int repeat) {
		Intent intent = new Intent(ACTION_SCHEDULE);
		intent.putExtra("tid", tid);
		intent.putExtra("action", action);
		intent.putExtra("time", time);
		intent.putExtra("repeat", repeat);
		PendingIntent pi = PendingIntent.getBroadcast(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pi;
	}
	
	private long getNextTime(String time, int repeat) {
		if (repeat < 0)
			return 0;
		if(repeat == 0 ){
			String[] times = time.split(" ");
			if(times.length == 2){
				String timeYMD = times[0];
				String timeHM  = times[1];
				String timenowYMD = SmartdevTools.getCurrentTimeYMD();
				String[] timeYMDs = timeYMD.split(":");
				int need_year = Integer.valueOf(timeYMDs[0]);
				int need_month = Integer.valueOf(timeYMDs[1]);
				int need_day = Integer.valueOf(timeYMDs[2]);
				String[] timeYMDs_now = timenowYMD.split(":");
				int year_now = Integer.valueOf(timeYMDs_now[0]);
				int month_now = Integer.valueOf(timeYMDs_now[1]);
				int day_now = Integer.valueOf(timeYMDs_now[2]);
				if(need_year < year_now )
					return 0;
				if((need_year == year_now) && (need_month < month_now) )
					return 0;
				if((need_year == year_now) && (need_month == month_now) && (need_day < day_now))
					return 0;
				int split = timeHM.indexOf(':');
				final int hour = Integer.parseInt(timeHM.substring(0, split));
				final int minute = Integer.parseInt(timeHM.substring(split+1, timeHM.length()));
				final long now = System.currentTimeMillis();
				Calendar cal = Calendar.getInstance();
				cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE,minute);
				cal.set(Calendar.SECOND, 0);
				if( cal.getTimeInMillis() < now){
						return 0;
				}
				else{
						return  cal.getTimeInMillis();
				}
			}
			else
				return 0;
		}
		final long now = System.currentTimeMillis();
		Calendar cal = Calendar.getInstance();
		final int day_of_week = cal.get(Calendar.DAY_OF_WEEK) - 1;
		
		try {
			int split = time.indexOf(':');
			final int hour = Integer.parseInt(time.substring(0, split));
			final int minute = Integer.parseInt(time.substring(split+1, time.length()));
			cal.setTimeZone(TimeZone.getTimeZone("GMT+8"));
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE,minute);
			cal.set(Calendar.SECOND, 0);
		} catch (Exception e) {
			Log.d(TAG, "格式化时间错误", e);
			return 0;
		}
		/* 周对齐 */
		int rep = (repeat >> day_of_week) | (repeat << (7-day_of_week));
		int i = 0;
		for (i=0; i<7; i++) {
			if (((rep & 1) > 0) && (cal.getTimeInMillis() > (now+1000)))
				break;
			rep = rep >> 1;
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		RobotDebug.d(NAME, "selectTime  :" + cal.getTimeInMillis());
		if (i<7)
			return cal.getTimeInMillis();
		else
			return 0;
	}
	
	private boolean setNextAlarm(SQLiteDatabase db) {
		boolean ret = true;
		Cursor cursor = null;
		try {
			cursor=db.rawQuery("select `tid`,`action`,`next`,`time`,`repeat` from `schedule` where `next`>0 and `enable`=1 and `repeat`>=0 order by `next` asc limit 1", null);
			if (cursor.getCount() > 0) {
				RobotDebug.d(NAME, "setNextAlarm 11111111111111");
				cursor.moveToFirst();
				pendingIntent = getPendingIntent(mContext, cursor.getInt(0), cursor.getString(1), cursor.getString(3), cursor.getInt(4));
				final long triggerAt = cursor.getLong(2);
				if (pendingIntent != null && triggerAt > 0){
					RobotDebug.d(NAME, "setNextAlarm 22222222222222");
					mAlarm.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
				}
				else {
					RobotDebug.d(NAME, "setNextAlarm 3333333333333333");
					ret = false;
					Log.d(TAG, "创建定时器失败");
				}
			} 
		} catch (Exception e) {
			ret = false;
			Log.d(TAG, "查找下一个定时器失败", e);
		} finally {
			if (cursor != null && !cursor.isClosed())
				cursor.close();
		}
		return ret;
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int tid = intent.getIntExtra("tid", 0);
			String action = intent.getStringExtra("action");
			String time = intent.getStringExtra("time");
			int repeat = intent.getIntExtra("repeat", -1);
			RobotDebug.d(NAME, " 有 任务要执行了  action : " + action);
			boolean needDo = false;
			if (action != null && action.length() > 2) {
				if(repeat == 0)
				{
					/*check time*/
					SimpleDateFormat simpleDateformat2 = new SimpleDateFormat("yyyy:MM:dd HH:mm");
					Date needDate2  = new Date(System.currentTimeMillis());
					String time2 = simpleDateformat2.format(needDate2);
					RobotDebug.d(TAG, "time2 : " + time2 + " time :" + time);
					if(time2.equals(time))
						needDo = true;
				}
				else if(repeat > 0)
				{
					SimpleDateFormat simpleDateformat2 = new SimpleDateFormat("HH:mm");
					Date needDate2  = new Date(System.currentTimeMillis());
					String time2 = simpleDateformat2.format(needDate2);
					RobotDebug.d(TAG, "time2 : " + time2 + " time :" + time);
					if(time2.equals(time))
						needDo = true;
				}
				/* 提交动作请求 */
				if(needDo)
					Robot.getInstance().getManager().toDo(action);
				Log.d(TAG, String.format("Todo schedule task action %s, time %s, now is %s",
						intent.getStringExtra("action"), intent.getStringExtra("time"), sdf.format(new Date(System.currentTimeMillis()))));
			}
			/* 更新定时器 */
			SQLiteDatabase db = null;
			try {
				db = mContext.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
				if (tid != 0 && repeat >=0) {
						final long triggerAt = getNextTime(time,repeat);
						int enable = 1;
						if(repeat == 0)
							enable = 0;
						String sql = String.format(Locale.getDefault(),
								"update `schedule` set `next`=%d,`enable`=%d where `tid`=%d", triggerAt,enable,tid);
						db.execSQL(sql);
				}
				setNextAlarm(db);
			} catch (Exception e) {
				Log.d(TAG, "更新定时任务失败", e);
			}finally {
				if (db != null && db.isOpen())
					db.close();
			}
		}
	};
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
	
	private String sql_escape(String s) {
		String s1 = s.replace("\"", "\"\"");
		String s2 = s1.replace("'", "''");
		return s2;
	}
}
