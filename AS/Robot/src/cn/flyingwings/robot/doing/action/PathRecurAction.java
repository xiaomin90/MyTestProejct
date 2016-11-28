package cn.flyingwings.robot.doing.action;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.xmppservice.XmppService;

public class PathRecurAction extends RobotAction {

	public static final String NAME = "path_recur";
	public String opcode;
	public String opt;
	public int pathId;
	public String pathName;
	public boolean isVoice = false;
	public String from = null;
	

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
		super.parse(s);
		try {
			JSONObject datas = new JSONObject(s);
			isVoice = datas.optBoolean("isVoice");
			opcode = datas.getString("opcode");
			opt = datas.optString("opt");
			from = datas.optString("from", null);
			if (isVoice) {
				pathId = datas.getInt("id");
				pathName = datas.getString("path_name");
			} else {
				JSONObject path = datas.optJSONObject("path");
				if(path != null) {
					pathId = path.optInt("id");
					pathName = path.optString("name");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void doing() {
		super.doing();
		//----------------------通知服务器--------------------------// 
		 try {
			DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
			JSONObject json = null;
			if(opt.equals("start")) {
					json = new JSONObject().put("todo","report_r_func_status")
									.put("curr_version", RobotVersion.current_system_sw_version)
									.put("function_code", "path_retrace")
									.put("function_status", "start")
									.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
									.put("phone_no", XmppService.phone_Number)
									.put("control_way",from==null?"app_button":from)
									.put("path_name",pathName);
				
			} else {
					json = new JSONObject().put("todo","report_r_func_status")
							.put("curr_version", RobotVersion.current_system_sw_version)
							.put("function_code", "path_retrace")
							.put("function_status", "end")
							.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
							.put("phone_no", XmppService.phone_Number)
							.put("end_reason","canceled");
			}
			service.sendDataToXmppService(json.toString());
		 } catch (JSONException e) {
				e.printStackTrace();
		 }
		//-----------------------通知服务器-------------------------//
		
	}

}
