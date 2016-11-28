package cn.flyingwings.robot.doing.action;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotErrorCode;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import cn.flyingwings.robot.xmppservice.XmppService;

public class SeekHome extends RobotAction {

	private String TAG = "SeekHome";
	public static String NAME = "seek_home";
	private int isSeekHome = -1; 
	private String from = null;
	
	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}
	
	@Override
	public String name() {
		return SeekHome.NAME;
	}
	
	@Override
	public boolean parse(String s) {
		try {
			Log.d(TAG,"SeekHome Action Parse :  " + s);
			JSONObject json = new JSONObject(s);
			isSeekHome = json.getInt("charge");
			from = json.optString("from", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.parse(s);
		return true;
	}
	
	private static final String result = "{\"opcode\":\"93401\",\"charge\":%d,\"result\":%d,\"error_msg\":\"%s\"}"; 
	
	@SuppressLint("DefaultLocale") 
	@Override
	public void doing() {
		Log.d(TAG, "seekHome aciton doing");
		super.doing();
		if ((isSeekHome != 1) && (isSeekHome != 0)) {
			Log.d(TAG, "option can not be recongined");
			return;
		}
		
		CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);		
		CANService.CANMessage canmsg = new CANService.CANMessage();
		byte[] arg = new byte[1];
		if (isSeekHome == 1) {
			arg[0] = 0x1;
			canmsg.set(0xE0, 0x00, arg);
		} else {
			arg[0] = 0x0;
			canmsg.set(0xE0, 0x00, arg);
		}
		
		String response = null;
		if (canservice.send(canmsg)){
			response = String.format(result, isSeekHome, 0, new String(""));
			//维拉电量不足20%，需要充电，开始寻找充电桩
			RobotStatusInfoService statusInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
			if(!statusInfoService.isDndMode()) {
				if ((statusInfoService.getBatteryLevel() < 20) && (from!=null) && from.equals("low_battery")) {
					try {
						Robot.getInstance().getManager().toDo(new JSONObject().put("name","say").put("content", "维拉电量不足20%，需要充电，开始寻找充电桩").toString());
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			response = String.format(result, isSeekHome, 1, RobotErrorCode.SEEKHOME_ERR);
		}		
		XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
		xmppservice.sendMsg("R2C", response, 93401);
		
		if (isSeekHome == 0) {
			Intent cancelSeekHome = new Intent("cn.flyingwings.robot.SeekHome.Cancel");
			Robot.getInstance().getContext().sendBroadcast(cancelSeekHome);
		}
		
		 //-------通知服务器----------------------//
		 try {
			DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
			JSONObject json = null;
			if(isSeekHome == 1) {
					json = new JSONObject().put("todo","report_r_func_status")
									.put("curr_version", RobotVersion.current_system_sw_version)
									.put("function_code", "find_charger")
									.put("function_status", "start")
									.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
									.put("phone_no", XmppService.phone_Number)
									.put("start_reason",from==null?"app_button":from);
				
			} else {
					json = new JSONObject().put("todo","report_r_func_status")
							.put("curr_version", RobotVersion.current_system_sw_version)
							.put("function_code", "find_charger")
							.put("function_status", "end")
							.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
							.put("phone_no", XmppService.phone_Number)
							.put("end_reason","canceled");
			}
			service.sendDataToXmppService(json.toString());
		 } catch (JSONException e) {
				e.printStackTrace();
		 }
		 //-------通知服务器---------------------//
	}
}
