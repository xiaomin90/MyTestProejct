package cn.flyingwings.robot.doing.action;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.apprtcservice.ApprtcService;
import cn.flyingwings.robot.service.CANService.CANMessage;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import cn.flyingwings.robot.service.UpdateCheckService;
import cn.flyingwings.robot.smartdeviceservice.Dev433Service;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

/**
 * 机器人状态信息
 * formart
 * {"name":"robot_info","subName":"app_inquiry"}
 * subName ---app_inquiry  app请求查询状态．
 * subName ---volume       app音量调节
 * subName ---head_limit   头部限位校准
 * @author min.js
 */
public class RobotInfoAction extends RobotAction {
	
	public static final String NAME = "robot_info";
	public String subName = null;
	public int volume = -1;
	public String volumeType = null;
	public String direction = null;
	
	@Override
	public int type() {
		return RobotAction.ACTION_TYPE_TASK;
	}
	
	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean parse(String s) {
		volume = -1;
		subName = null;
		try {
			JSONObject data_json = new JSONObject(s);
			subName = data_json.optString("subName");
			if(subName.equals("volume"))
			{
				volume = data_json.optInt("volume");
				volumeType = data_json.optString("type", null);
			}
			else if(subName.equals("head_limit"))
			{
				direction = data_json.optString("direction");
			}
		} catch (JSONException e) {
			RobotDebug.d(NAME, "get subname failed e : " + e.toString());
		}
		super.parse(s);
		return true;
	}
	
	@Override
	public void doing() {
		RobotDebug.d(NAME, "查询状态 1... subName: " + subName);
		if((subName != null)  && subName.equals("app_inquiry")) {
			/**
			 * 查询机器人状态
			 */
			RobotDebug.d(NAME, "查询状态 2... subName: " + subName);
			JSONObject datas = new JSONObject();
			try {
				datas.put("opcode", "93001");
				datas.put("result", 0);
				datas.put("battery", (int)RobotStatusInfoService.batteryLevel);
				RobotStatusInfoService robotInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
				datas.put("wifi", robotInfoService.getWifilevel());
				if(Dev433Service.ONSercurity)
					datas.put("safe", "use");
				else
					datas.put("safe", "cancel");
				int volume = robotInfoService.getvolume(true);
				datas.put("volume", volume);
				if( Robot.getInstance().getManager().getCurrentAffair() !=null)
				datas.put("status", Robot.getInstance().getManager().getCurrentAffair());
			} catch (JSONException e) {
				RobotDebug.d(NAME, "Robot info app_inquiry failed e: " + e.toString());
			}
			XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			xmppService.sendMsg("R2C", datas.toString(), 93001);
			
			/**
			 * 93001消息也是作为进入亲情活动页面后的第一条消息，根据该消息进行提示升级
			 */
			/* 该标志表示需要通知客户端是否进行升级*/
			if(ApprtcService.isNeedInfoUpdate) {
				ApprtcService.isNeedInfoUpdate = false;
				UpdateCheckService updateCheckService = (UpdateCheckService) Robot.getInstance().getService(UpdateCheckService.NAME);
				if (updateCheckService.needUpdateModule.get("kernel").downLoadOK
						&& !updateCheckService.needUpdateModule.get("kernel").update.equals(new String("2"))
						&& updateCheckService.needUpdateModule.get("system").downLoadOK
						&& !updateCheckService.needUpdateModule.get("system").update.equals(new String("2"))
						&& updateCheckService.needUpdateModule.get("ramdisk").downLoadOK
						&& !updateCheckService.needUpdateModule.get("ramdisk").update.equals(new String("2"))) {
					JSONObject info_data = new JSONObject();
					try {
						info_data.put("opcode", "97101");
						xmppService.sendMsg("R2C", info_data.toString(), 97101);
						// 不在提示提示音
						RobotDebug.d(NAME, "sendMsg:97101 提示APP管理员有新版本更新");
					} catch (JSONException e) {
						RobotDebug.d(NAME, "info  update e:" + e.toString());
					}
				}
			}
		}
		else if((subName != null)  && subName.equals("volume")) {
			/**
			 * 设置音量
			 */
			RobotStatusInfoService robotInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
			XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			try {
				if(robotInfoService.setvolume(volume,volumeType)) {
					xmppservice.sendMsg("R2C", new JSONObject().put("opcode", "93701").put("result", 0).put("volume",volume).put("type",volumeType==null?null:volumeType).toString(), TransferCode.robot_system_volume);
				} else {
					xmppservice.sendMsg("R2C", new JSONObject().put("opcode", "93701").put("result", 1).put("volume",volume).put("type",volumeType==null?null:volumeType).put("error_msg", "音量类型不正确").toString(), TransferCode.robot_system_volume);
				}
			} catch (JSONException e) {
				RobotDebug.d(NAME, "设置音量   json e: " +e.toString());
			}
		}
		else if((subName != null) && subName.equals("head_limit")) {
			if(direction == null)
				return;
			CANMessage msg = null;
			if(direction.equals("left")){
				msg = new CANMessage(0x740, 0, new byte[]{0});
			}
			else if(direction.equals("right")){
				msg = new CANMessage(0x740, 0, new byte[]{1});
			}
			else if(direction.equals("up")) {
				msg = new CANMessage(0x740, 0, new byte[]{2});
			}
			else if(direction.equals("down")) {
				msg = new CANMessage(0x740, 0, new byte[]{3});
			}
			if(msg == null)
				return;
			CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
			if(!canservice.send(msg)) {
				XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
				RobotDebug.d(NAME,"head_limit send msg failed.");
				try {
					xmppservice.sendMsg("R2C", new JSONObject().put("opcode","97201").put("direction",direction)
					       .put("result", 1).put("error_msg", "通知机器人底盘失败").toString(), 97201);
				} catch (JSONException e) {
					RobotDebug.d(NAME, "head_limit send canMsg failed."+e.toString());
				}
			}
		}
		else if((subName != null) && subName.equals("head_limit_save")) {
			CANMessage msg = new CANMessage(0x740, 0, new byte[]{4});
			CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
			if(!canservice.send(msg)) {
				XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
				RobotDebug.d(NAME,"head_limit_save send msg failed.");
				try {
					xmppservice.sendMsg("R2C", new JSONObject().put("opcode","97202").put("result", 1).put("error_msg", "通知机器人底盘失败").toString(), 97202);
				} catch (JSONException e) {
					RobotDebug.d(NAME, "head_limit send canMsg failed."+e.toString());
				}
			}
		}
	}
}
