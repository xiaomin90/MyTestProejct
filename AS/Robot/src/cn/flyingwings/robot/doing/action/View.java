package cn.flyingwings.robot.doing.action;

import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.apprtcservice.ApprtcService;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.chatService.ModuleController.State;
import cn.flyingwings.robot.service.WakeupService;
import cn.flyingwings.robot.xmppservice.XmppService;
import cn.flyingwings.robot.zhumu.ZhuMuService;
import cn.flyingwings.robot.zhumu.ZhuMuService.ZHUMUMeetingType;

import com.android.zhumu.ZhuMuStatus.MeetingStatus;

public class View extends RobotAction {
	
	public static final String NAME = "view";

	private boolean videoOn;
	
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
			videoOn = (json.getInt("video") != 2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.parse(s);
		return true;
	}
	
	private static final String result = "{\"opcode\":92902,\"status\":\"%s\",\"video\":%d,\"audio\":%d}";
	@Override
	public void doing() {
		if(ClientControl.session_mode != null && ClientControl.session_mode.equals("WEBRTC")) {
			ApprtcService rtc = (ApprtcService) Robot.getInstance().getService(ApprtcService.NAME);
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			if(videoOn) {
				rtc.startVideo();
				rtc.stopAudio();
				xmpp.sendMsg("R2C", String.format(result,"success",1,2), 92902);
			} else {
				rtc.stopAudio();
				rtc.stopVideo();
				xmpp.sendMsg("R2C", String.format(result,"success",2,2), 92902);
			}
		} else  if (ClientControl.session_mode != null && ClientControl.session_mode.equals("ZHUMU")) {
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			ZhuMuService zhumu = (ZhuMuService) Robot.getInstance().getService(ZhuMuService.NAME);
			if (videoOn) {
				if((ZhuMuService.status == MeetingStatus.MEETING_STATUS_IDLE) && (ZhuMuService.isMeetingSDKInitOK == true)) {
					RobotDebug.d(NAME, "进入会议   ClientControl.ROOMID : " + ClientControl.ROOMID + "  XmppService.auth_Key " + XmppService.auth_Key);
					//RobotAudioRecorder.getInstance().releaseRecord();
					ModuleController.getInstance().changeState(State.CLOSE, null);
					xmpp.sendMsg("R2C", String.format(result, "success",1,2), 92902);
					zhumu.joinRoom(ClientControl.ROOMID, XmppService.auth_Key,ZHUMUMeetingType.VIEW);
				} else {
					RobotDebug.d(NAME, "ClientControl.ROOMID : " + ClientControl.ROOMID + "  XmppService.auth_Key   : " + XmppService.auth_Key + " 机器人还没有退出会议拒绝该次会议");
					xmpp.sendMsg("R2C", String.format(result,"fail",2,2), 92902);
					zhumu.leaveRoom();
				}
			} else {
				RobotDebug.d(NAME, "Leave room ClientControl.ROOMID : " + ClientControl.ROOMID + "  XmppService.auth_Key " + XmppService.auth_Key);
				zhumu.leaveRoom();
				if((ZhuMuService.status == MeetingStatus.MEETING_STATUS_IDLE) || (ZhuMuService.isMeetingSDKInitOK == false)) {
					WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
					wakeupService.startWakeup();
					xmpp.sendMsg("R2C", String.format(result,"success",2,2), 92902);
				}
			}
		}
	}
}
