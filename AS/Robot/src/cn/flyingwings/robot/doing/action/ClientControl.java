package cn.flyingwings.robot.doing.action;

import org.json.JSONObject;

import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.apprtcservice.ApprtcService;
import cn.flyingwings.robot.apprtcservice.ApprtcService.VideoQulity;
import cn.flyingwings.robot.xmppservice.XmppService;
import cn.flyingwings.robot.zhumu.ZhuMuService;

/**
 * 控制客户端状态并维护客户端状态。<br>
 * 命令参数：
 * name=client_control, room_id=&lt;String&gt;, quality=[1, 2], state=[connect, disconnect, resume]
 * @author gd.zhang@flyingwings.cn
 *
 */
public class ClientControl extends RobotAction {

	public static final String NAME = "client_control";
	
	public static final String ACTION_RESUME = "{\"name\":\"client_control\",\"todo\":\"0\",\"session_mode\":\"0\",\"apprtc_server\":\"0\",\"room_id\":\"0\",\"state\":\"connect\"}";
	
	private static final String KEY_TODO = "todo";
	private static final String KEY_SEESSION_MOD = "session_mode";
	private static final String KEY_APPRTC_SERVER = "apprtc_server";
	private static final String KEY_ROOM_ID = "room_id";
	private static final String KEY_QUALITY = "quality";
	private static final String KEY_STATE	= "state";
	
	private String todo;
	public  static  String session_mode;
	private String apprtc_server;
	private String roomId;
	private int quality;
	public String state;
	
	private static final VideoQulity qua_list[] = {VideoQulity.HD, VideoQulity.SHD, VideoQulity.HD};
	
	public static String ROOMID = null;
	
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
		Log.d(NAME, s);
		try {
			JSONObject json = new JSONObject(s);
			todo = json.getString(KEY_TODO);
			apprtc_server = json.getString(KEY_APPRTC_SERVER);
			state = json.getString(KEY_STATE);
			if(state != null && state.equals("connect")) {
				session_mode = json.getString(KEY_SEESSION_MOD);
				roomId = json.getString(KEY_ROOM_ID);
				ROOMID = roomId;
			}
		} catch (Exception e) {
			
		}
		return super.parse(s);
	}
	
	@Override
	public void doing() {
		Log.d(NAME, "state is "+ state);
		if(session_mode != null && session_mode.equals("WEBRTC")) {
			ApprtcService rtc = (ApprtcService) Robot.getInstance().getService(ApprtcService.NAME);
			if ("connect".equals(state)) {
				rtc.startConnect(apprtc_server,ROOMID);
			} else if ("disconnect".equals(state)){
				rtc.disconnect();
			} else {
				RobotDebug.d(NAME, "unkown state : " + state);
			}
		} else if (session_mode != null && session_mode.equals("ZHUMU")) {
			ZhuMuService zhumu = (ZhuMuService) Robot.getInstance().getService(ZhuMuService.NAME);
			if ("disconnect".equals(state)) {
				zhumu.leaveRoom();
			}
		} else {
			RobotDebug.d(NAME,"unkown Session mode.");
		}
		super.doing();
	}

}
