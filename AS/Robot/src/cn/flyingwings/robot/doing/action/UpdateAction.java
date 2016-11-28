package cn.flyingwings.robot.doing.action;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.service.UpdateCheckService;

/**
 * update 
 * @author min.js
 *json format {“name”:“update”, "reason":"xxx"} 本地调用
 *json format {“name”:"update","reason":"from_app"} app调用
 */
public class UpdateAction  extends RobotAction{

	public static String NAME = "update";
	private boolean force = false;
	private String rebootReason = null;
	
	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public boolean parse(String s) {
		
		try {
			JSONObject json = new JSONObject(s);
			rebootReason = json.getString("reason");
			force = json.optBoolean("force", false);
		} catch (JSONException e) {
			RobotDebug.d(NAME, "parse  reboot reason not json format e: " + e.toString());
		}
		return super.parse(s);
	}
	
	@Override
	public void doing() {
		RobotDebug.d(NAME,"reboot system now reason 1: " + rebootReason);
		Context context = Robot.getInstance().getContext();
		Activity temp = (Activity)context;
		temp.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(rebootReason.equals(new String("from_app")))
				{
					UpdateCheckService updateservice = (UpdateCheckService) Robot.getInstance().getService(UpdateCheckService.NAME);
					updateservice.allowed(true);
				}
				else
				{
					//----------- 通知服务器关机 ----原因------start-----------// 
					DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
					try {
						service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
								.put("function_code", "startup_shutdown").put("function_status", "end")
								.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
								.put("end_reason", "version_update").toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//----------- 通知服务器关机 ----原因------end-----------// 
					UpdateCheckService update_check_service = (UpdateCheckService) Robot.getInstance().getService(UpdateCheckService.NAME);
					if(!force) 
						update_check_service.sayInfo("准备升级,等待升级成功后重启.升级过程中和重启过程请勿断电");
					else 
						update_check_service.sayInfo("维拉有重大更新，准备升级,升级过程中和重启过程请勿断电");
					try {
							Thread.sleep(15000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					Intent intent_powoff = new Intent("android.intent.action.otaforrobot");
					intent_powoff.putExtra("reason", rebootReason);
					intent_powoff.getStringExtra("reason");
					Robot.getInstance().getContext().sendBroadcast(intent_powoff);
				}
			}
		});
		return;
	}

}
