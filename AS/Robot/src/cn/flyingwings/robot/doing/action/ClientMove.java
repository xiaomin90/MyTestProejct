package cn.flyingwings.robot.doing.action;

import org.json.JSONObject;

import android.util.Log;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.CANService.CANMessage;

public class ClientMove extends RobotAction {

	private String TAG = "ClientMove";
	public static String NAME = "client_move";
	private String json_format = "{\"opt\":%d,\"act\":%d,\"left_speed\":%d,\"right_speed\":%d,\"header_h\":%d,\"header_v\":%d,\"ang\":%d,\"distance\":%d}";
	private static final String KEY_OPT = "opt";
	private static final String KEY_ACT = "act";
	private static final String KEY_LEFT_SPEED = "left_speed";
	private static final String KEY_RIGHT_SPEED = "right_speed";
	private static final String KEY_HEADER_H = "header_h";
	private static final String KEY_HEADER_V = "header_v";
	private static final String KEY_ANG = "ang";
	private static final String KEY_DISTANCE = "distance";

	private int opt = 0;
	private int act = 0;
	private short left_speed = (short) 0;
	private short right_speed = (short) 0;
	private short header_h = (short) 0;
	private short header_v = (short) 0;
	private short ang = (short) 0;
	private short distance = (short) 0;
	
	// 数据统计
	private String from = null;
	

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}

	@Override
	public void doing() {
		CANMessage msg = null;
		DataStatisticsService statisticsService = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
		if ((this.opt == 1) && (this.act == 0)) { 
			/*
			 * @头部运动 CAN : func: 0x120 cmd:0x1
			 */
			byte[] data = new byte[4];
			data[0] = (byte) ((this.header_h >> 8) & 0x00ff);
			data[1] = (byte) (Math.abs(this.header_h) & 0x00ff);
			data[2] = (byte) ((this.header_v >> 8) & 0x00ff);
			data[3] = (byte) (Math.abs(this.header_v) & 0x00ff);
			RobotDebug.d(TAG, "in 头部运动 CAN : func: 0x120  cmd:0x1 h:" + this.header_h + " v:" + this.header_v
					+ " data0-4 : " + data[0] + "  " + data[1] + "  " + data[2] + "  " + data[3]);
			msg = new CANMessage(0x120, 0x1, data);
			//chassis/head
			//--------通知服务器-------------------//
			statisticsService.motionCount("head", from==null?"app_button":from);
			//--------通知服务器-------------------//
		} else if ((this.opt == 2) && (this.act == 0)) {
			/*
			 * @ 底盘运动CAN : func:0x100 cmd: 0x8
			 */
			//--------通知服务器-------------------//
			statisticsService.motionCount("chassis", from==null?"app_button":from);
			//--------通知服务器-------------------//
			byte[] data = new byte[4];
			data[0] = (byte) ((this.left_speed >> 8) & 0x00ff);
			data[1] = (byte) (Math.abs((this.left_speed)) & 0x00ff);
			data[2] = (byte) ((this.right_speed >> 8) & 0x00ff);
			data[3] = (byte) (Math.abs(this.right_speed) & 0x00ff);
			if (this.left_speed != this.right_speed) {
				// 旋转
				byte[] sendData = new byte[6];
				sendData[0] = data[0];
				sendData[1] = data[1];
				sendData[2] = data[2];
				sendData[3] = data[3];
				sendData[4] = (byte) ((this.ang >> 8) & 0x00ff);
				sendData[5] = (byte) (Math.abs((this.ang)) & 0x00ff);
				msg = new CANMessage(0x100, 0x6, sendData);
				RobotDebug.d(TAG,
						"in 底盘运动CAN 旋转: func:0x100 cmd: 0x6 l:" + this.left_speed + " r:" + this.right_speed
								+ " data0-5 : " + sendData[0] + "  " + sendData[1] + "  " + sendData[2] + "  "
								+ sendData[3] + " " + sendData[4] + " " + sendData[5]);
			} else if ((this.left_speed == this.right_speed) && (this.right_speed > 0)) {
				// 前进
				byte[] sendData = new byte[4];
				sendData[0] = data[1];
				sendData[1] = data[3];
				sendData[2] = (byte) ((this.distance >> 8) & 0x00ff);
				sendData[3] = (byte) (Math.abs((this.distance)) & 0x00ff);
				msg = new CANMessage(0x100, 0x2, sendData);
				RobotDebug.d(TAG, "in 底盘运动CAN 前进: func:0x100 cmd: 0x2 l:" + this.left_speed + " r:" + this.right_speed
						+ " data0-3 : " + sendData[0] + "  " + sendData[1] + "  " + sendData[2] + "  " + sendData[3]);
			} else if ((this.left_speed == this.right_speed) && (this.right_speed < 0)) {
				// 后退
				byte[] sendData = new byte[4];
				sendData[0] = data[1];
				sendData[1] = data[3];
				sendData[2] = (byte) ((this.distance >> 8) & 0x00ff);
				sendData[3] = (byte) (Math.abs((this.distance)) & 0x00ff);
				msg = new CANMessage(0x100, 0x4, sendData);
				RobotDebug.d(TAG, "in 底盘运动CAN 后退: func:0x100 cmd: 0x4 l:" + this.left_speed + " r:" + this.right_speed
						+ " data0-3 : " + sendData[0] + "  " + sendData[1] + "  " + sendData[2] + "  " + sendData[3]);
			} else {
				msg = new CANMessage(0x100, 0x8, data); // old protocol
				RobotDebug.d(TAG,
						"in 底盘运动CAN old协议: func:0x100 cmd: 0x8 l:" + this.left_speed + " r:" + this.right_speed
								+ " data0-3 : " + data[0] + "  " + data[1] + "  " + data[2] + "  " + data[3]);
			}
		} else if (act == 1) {
			/*
			 * @停止运动 :
			 * 
			 * @body stop CAN: func:0x100 cmd: 0x0
			 * 
			 * @header stop CAN: func:0x120 cmd: 0x0
			 */
			RobotDebug.d(TAG, "in 停止运动 ");
			byte[] data = new byte[0];
			msg = new CANMessage(0x120, 0x0, data);// header.
			CANService service = (CANService) Robot.getInstance().getService("CAN");
			if (service.send(msg) == true){
				RobotDebug.d(TAG, "clientMove Send header stop success");
				PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
			    pathService.addstep(msg);
			}
				
			else
				RobotDebug.d(TAG, "clientMove Send header stop failed");
			msg = new CANMessage(0x100, 0x0, data);// body
			if (service.send(msg) == true){
				RobotDebug.d(TAG, "clientMove Send body stop success");
				PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
			    pathService.addstep(msg);
			}else
				RobotDebug.d(TAG, "clientMove Send body stop failed");
			return;
		} else if (act == 6) {
			/*
			 * @头部回中CAN : func: 0x120 cmd: 0x8
			 */
			RobotDebug.d(TAG, "头部回中CAN : func: 0x120  cmd: 0x8");
			byte[] data = new byte[0];
			msg = new CANMessage(0x120, 0x8, data);
		} else {
			Log.e(TAG, "clientMove doing  Move can not be identity.");
			return;
		}

		CANService service = (CANService) Robot.getInstance().getService("CAN");
		if (service.send(msg) == true){
			RobotDebug.d(TAG, "clientMove Send CAN Msg success");
		    PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		    pathService.addstep(msg);
		}else{
			RobotDebug.d(TAG, "clientMove Send CAN Msg Failed");
		}
		super.doing();
	}

	@Override
	public boolean parse(String s) {
		RobotDebug.d(TAG, "in Client Move  parse :" + s);
		this.opt = 0;
		this.act = 0;
		this.left_speed = (short) 0;
		this.right_speed = (short) 0;
		this.header_h = (short) 0;
		this.header_v = (short) 0;
		this.distance = (short) 0;
		this.ang = (short) 0;
		try {
			JSONObject json = new JSONObject(s);
			this.opt = json.getInt(KEY_OPT);
			this.act = json.getInt(KEY_ACT);
			this.left_speed = (short) json.getInt(KEY_LEFT_SPEED);
			this.right_speed = (short) json.getInt(KEY_RIGHT_SPEED);
			this.header_h = (short) json.getInt(KEY_HEADER_H);
			this.header_v = (short) json.getInt(KEY_HEADER_V);
			this.ang = (short) json.getInt(KEY_ANG);
			this.distance = (short) json.getInt(KEY_DISTANCE);
			this.from = json.optString("from", null);
		} catch (Exception e) {
			Log.e(TAG, "in parse  string not json format");
		}
		return super.parse(s);
	}
}
