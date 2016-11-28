/**
 * 
 */
package cn.flyingwings.robot.doing.action;

import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import cn.flyingwings.robot.service.ScheduleService;

/**
 * @author WangBaoming
 *
 */
public class DndAction extends RobotAction {
	public final static String NAME = "do_not_disturb";
	
	private final static int repeat = (ScheduleService.MONDAY | ScheduleService.TUESDAY | ScheduleService.WEDNESDAY | ScheduleService.THURSAY | ScheduleService.FRIDAY | 
			ScheduleService.SATURDAY | ScheduleService.SUNDAY);
	
	private JSONObject mParams = null;
	
	@Override
	public int type() {
		return ACTION_TYPE_SIMPLE;
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public boolean parse(String s) {
		super.parse(s);
		try {
			Log.d(NAME, "parse: " + s);
			JSONObject jobj = new JSONObject(s);
			mParams = jobj; 
			mParams.remove("name");
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private Date parseDate(String string) throws ParseException{
		if(null == string) throw new ParseException("date parse error", -1);
		
		Date date = new Date();
		String[] time = string.split(":");
		if(2 != time.length){
			throw new ParseException("date parse error", -1);
		}
		
		try {
			date.setHours(Integer.valueOf(time[0]));
			date.setMinutes(Integer.valueOf(time[1]));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new ParseException("date parse error", -1);
		}
		
		return date;
	}
	
	@Override
	public void doing() {
		super.doing();
		Log.d(NAME, "param: " + mParams.toString());
		
		ScheduleService scheduleService = (ScheduleService) Robot.getInstance().getService(ScheduleService.NAME);
		RobotStatusInfoService robotInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
		DndPreference dndPreference = new DndPreference(Robot.getInstance().getContext());
		
		try {
			boolean successful = true;
					
			String opt = mParams.getString("opt");
			if("set".equals(opt)){
				String from = mParams.getString("from");
				String to = mParams.getString("to");
				
				int fromTid = scheduleService.addTaskForApp("DND_ENTER", new JSONObject().put("name", NAME).put("opt", "enter").toString(), from, repeat);
				if(-1 == fromTid){
					successful = false;
					Log.e(NAME, "error: add enter task");
					return;
				}
				
				int toTid = scheduleService.addTaskForApp("DND_EXIT", new JSONObject().put("name", NAME).put("opt", "exit").toString(), to, repeat);
				if(-1 == toTid){
					successful = false;
					
					scheduleService.delTask(fromTid);
					Log.e(NAME, "error: add exit task");
					return;
				}
				
				// remove the old DND task if exiting
				scheduleService.delTask(dndPreference.getDndEnterTid());
				scheduleService.delTask(dndPreference.getDndExitTid());
				
				dndPreference.saveDndEnterTid(fromTid);
				dndPreference.saveDndExitTid(toTid);		
				dndPreference.saveDndStartTime(from);
				dndPreference.saveDndStopTime(to);
				
				dndPreference.enableDnd(true);
				
				// check whether to enter/exit dnd action now
				try {
					Date start = parseDate(from);
					Date end = parseDate(to);
					Date now = new Date();
					if(start.before(end)){
						if(now.compareTo(start) >= 0 && now.before(end)){ // should in Dnd Mode
							if(!robotInfoService.isDndMode()){
								Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "enter").toString());
							}
						}else{ // should not in Dnd Mode
							if(robotInfoService.isDndMode()){
								Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
							}
						}
					}else if(start.after(end)){
						if(now.compareTo(end) >= 0 && now.before(start)){ // should not in Dnd Mode
							if(robotInfoService.isDndMode()){
								Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
							}
						}else{ // should in Dnd Mode
							if(!robotInfoService.isDndMode()){
								Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "enter").toString());
							}
						}
					}else{ // if equal, disable dnd mode.
						if(robotInfoService.isDndMode()){
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}else if("on".equals(opt)){
				if(!dndPreference.isDndEnabled()){
					String from = dndPreference.getDndStartTime();
					String to = dndPreference.getDndStopTime();
					
					int fromTid = scheduleService.addTaskForApp("DND_ENTER", new JSONObject().put("name", NAME).put("opt", "enter").toString(), from, repeat);
					if(-1 == fromTid){
						successful = false;
						Log.e(NAME, "error: add enter task");
						return;
					}
					
					int toTid = scheduleService.addTaskForApp("DND_EXIT", new JSONObject().put("name", NAME).put("opt", "exit").toString(), to, repeat);
					if(-1 == toTid){
						successful = false;
						
						scheduleService.delTask(fromTid);
						Log.e(NAME, "error: add exit task");
						return;
					}

					dndPreference.saveDndEnterTid(fromTid);
					dndPreference.saveDndExitTid(toTid);	
					
					dndPreference.enableDnd(true);
					
					// check whether to enter/exit dnd action now
					try {
						Date start = parseDate(from);
						Date end = parseDate(to);
						Date now = new Date();
						if(start.before(end)){
							if(now.compareTo(start) >= 0 && now.before(end)){ // should in Dnd Mode
								if(!robotInfoService.isDndMode()){
									Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "enter").toString());
								}
							}else{ // should not in Dnd Mode
								if(robotInfoService.isDndMode()){
									Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
								}
							}
						}else if(start.after(end)){
							if(now.compareTo(end) >= 0 && now.before(start)){ // should not in Dnd Mode
								if(robotInfoService.isDndMode()){
									Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
								}
							}else{ // should in Dnd Mode
								if(!robotInfoService.isDndMode()){
									Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "enter").toString());
								}
							}
						}else{ // if equal, disable dnd mode.
							if(robotInfoService.isDndMode()){
								Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
							}
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}else if("off".equals(opt)){
				if (dndPreference.isDndEnabled()) {
					scheduleService.delTask(dndPreference.getDndEnterTid());
					scheduleService.delTask(dndPreference.getDndExitTid());

					dndPreference.enableDnd(false);
					
					// if dnd, exit dnd action now
					if(robotInfoService.isDndMode()){
						Robot.getInstance().getManager().toDo(new JSONObject().put("name", NAME).put("opt", "exit").toString());
					}
				}
			}else{ // alarm callback				
				if("enter".equals(opt)){ // enter DND mode
					robotInfoService.setDndMode(true);
					Log.d(NAME, "enter DND");
				}else if("exit".equals(opt)){ // exit DND mode
					robotInfoService.setDndMode(false);
					Log.d(NAME, "exit DND");
				}else{
					Log.e(NAME, "not support opt: " + opt);
				}
			}
			
			// reply App
			Log.d(NAME, "successful: " + successful);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static class DndPreference {
		private final static String PREF_NAME = "DND";
		
		private final static String PREF_FROM = "from";
		private final static String PREF_TO = "to";

		private final static String PREF_ENTER_TID = "exit_tid";
		private final static String PREF_EXIT_TID = "enter_tid";		

		private final static String PREF_DND_ENABLED = "DND_ON";
		
		private SharedPreferences mPreference;
		private Editor mEditor;
		
		public DndPreference(Context context) {
			mPreference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
			mEditor = mPreference.edit();
		}
		
		public void saveDndStartTime(String from){
			mEditor.putString(PREF_FROM, from);
			mEditor.commit();
		}

		public void saveDndStopTime(String to){
			mEditor.putString(PREF_FROM, to);
			mEditor.commit();
		}
		
		public void saveDndEnterTid(int tid){
			mEditor.putInt(PREF_ENTER_TID, tid);
			mEditor.commit();
		}
		
		public void saveDndExitTid(int tid){
			mEditor.putInt(PREF_EXIT_TID, tid);
			mEditor.commit();
		}
		
		public String getDndStartTime(){
			return mPreference.getString(PREF_FROM, "");
		}		

		public String getDndStopTime(){
			return mPreference.getString(PREF_TO, "");
		}
		
		public int getDndEnterTid(){
			return mPreference.getInt(PREF_ENTER_TID, -1);
		}
		
		public int getDndExitTid(){
			return mPreference.getInt(PREF_EXIT_TID, -1);
		}
		
		public void enableDnd(boolean enabled){
			mEditor.putBoolean(PREF_DND_ENABLED, enabled);
			mEditor.commit();
		}
		
		public boolean isDndEnabled(){
			return mPreference.getBoolean(PREF_DND_ENABLED, false);
		}
	}

}
