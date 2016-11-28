package cn.flyingwings.robot.doing.affair;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.zhumu.ZhuMuStatus.MeetingStatus;

import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.apprtcservice.ApprtcService;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.WakeupService;
import cn.flyingwings.robot.xmppservice.XmppService;
import cn.flyingwings.robot.zhumu.ZhuMuService;

public class Call extends RobotAffair {

	public static final String NAME = "call";
	private String TAGS = "CAllAffair";
	@Override
	public String name() {
		return NAME;
	}

	public Call(RobotAffairManager am) {
		super(am);
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		Log.d(TAGS,"apprtcAffair Start ");
	}
	
	@Override
	protected void onFinished() {
		Log.d(TAGS, "apprtcAffair is finished.");
		if(cn.flyingwings.robot.doing.action.ClientControl.session_mode != null && (cn.flyingwings.robot.doing.action.ClientControl.session_mode.equals("WEBRTC")) ){
			ApprtcService apprtcService = (ApprtcService) Robot.getInstance().getService(ApprtcService.NAME);
			apprtcService.stopAudio();
			apprtcService.stopVideo();
			WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
			wakeupService.startWakeup();
		} else if(cn.flyingwings.robot.doing.action.ClientControl.session_mode != null && (cn.flyingwings.robot.doing.action.ClientControl.session_mode.equals("ZHUMU")) ) {
			ZhuMuService zhumu = (ZhuMuService) Robot.getInstance().getService(ZhuMuService.NAME);
			zhumu.leaveRoom();
			//由于瞩目SDK有可能目前存在可能不会回调的问题，所以添加超时机制，等待瞩目sdk完善后,调整以下代码
			int count = 20;
			int index = 0;
			while(ZhuMuService.status != MeetingStatus.MEETING_STATUS_IDLE) {
				index++;
				if(index >= count)
					break;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(ZhuMuService.status == MeetingStatus.MEETING_STATUS_IDLE) {
				RobotDebug.d(TAGS,"开启唤醒");
				WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
				wakeupService.startWakeup();
			}
		}
	}

}
