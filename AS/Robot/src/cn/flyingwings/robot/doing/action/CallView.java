package cn.flyingwings.robot.doing.action;

import java.io.IOException;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.apprtcservice.ApprtcService;
import cn.flyingwings.robot.apprtcservice.ApprtcService.ConnectionStatus;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.chatService.ModuleController.State;
import cn.flyingwings.robot.service.WakeupService;
import cn.flyingwings.robot.xmppservice.XmppService;
import cn.flyingwings.robot.zhumu.ZhuMuService;
import cn.flyingwings.robot.zhumu.ZhuMuService.ZHUMUMeetingType;

import com.android.zhumu.ZhuMuStatus.MeetingStatus;

public class CallView extends RobotAction {

	public static String NAME = "call_view";
	
	private boolean videoOn;
	private boolean audioOn;
	
	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}
	
	@Override
	public boolean parse(String s) {
		try {
			RobotDebug.d(NAME, "call_view 111");
			JSONObject json = new JSONObject(s);
			videoOn = (json.getInt("video") != 2);
			audioOn = (json.getInt("audio") != 2);
		} catch (Exception e) {
		}
		super.parse(s);
		return true;
	}
	
	public static final String result = "{\"opcode\":\"92902\",\"status\":\"%s\",\"video\":%d,\"audio\":%d}";
	
	@Override
	public void doing() {
		super.doing();
		RobotDebug.d(NAME, "call_view : " + ClientControl.session_mode);
		if(ClientControl.session_mode != null && ClientControl.session_mode.equals("WEBRTC")) {
				ApprtcService rtc = (ApprtcService) Robot.getInstance().getService(ApprtcService.NAME);
				XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
				if (videoOn && audioOn) {
					/*释放audio资源*/
					//RobotAudioRecorder.getInstance().releaseRecord();
					ModuleController.getInstance().changeState(State.CLOSE, null);
					if(ApprtcService.status != ConnectionStatus.Connected){
						xmpp.sendMsg("R2C", String.format(result,"fail", 1,1), 92902);
						/*获取audio资源*/
						WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
						wakeupService.startWakeup();
						return;
					}
					else
						xmpp.sendMsg("R2C", String.format(result,"success", 1,1), 92902);
					rtc.startAudio();
					rtc.startVideo();
					
				} else {
					rtc.stopAudio();
					rtc.stopVideo();
					if(ApprtcService.status != ConnectionStatus.Connected)
						xmpp.sendMsg("R2C", String.format(result,"fail", 2,2), 92902);
					else
						xmpp.sendMsg("R2C", String.format(result,"success",2,2), 92902);
					/*获取audio资源*/
					WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
					wakeupService.startWakeup();
				}
		} else if(ClientControl.session_mode != null && ClientControl.session_mode.equals("ZHUMU")) {
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			ZhuMuService zhumu = (ZhuMuService) Robot.getInstance().getService(ZhuMuService.NAME);
			if (videoOn && audioOn) {
				if((ZhuMuService.status == MeetingStatus.MEETING_STATUS_IDLE) && (ZhuMuService.isMeetingSDKInitOK == true)) {
					RobotDebug.d(NAME, "进入会议   ClientControl.ROOMID : " + ClientControl.ROOMID + "  XmppService.auth_Key " + XmppService.auth_Key);
					//RobotAudioRecorder.getInstance().releaseRecord();
					ModuleController.getInstance().changeState(State.CLOSE, null);
					String musicpath = "sound/remote_video.mp3";//path
					palyrecord(musicpath);
					xmpp.sendMsg("R2C", String.format(result,"success", 1,1), 92902);
					zhumu.joinRoom(ClientControl.ROOMID, XmppService.auth_Key,ZHUMUMeetingType.VIDEO_AUIDO);
				} else {
					RobotDebug.d(NAME, "ClientControl.ROOMID : " + ClientControl.ROOMID + "  XmppService.auth_Key   : " + XmppService.auth_Key + " 机器人还没有退出会议拒绝该次会议");
					xmpp.sendMsg("R2C", String.format(result,"fail", 2,2), 92902);
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
	
	public void palyrecord(String path){
		MediaPlayer mediaplayer = null;
		try{
			mediaplayer = new MediaPlayer();
			Context  context = Robot.getInstance().getContext();
			AssetManager assMg = context.getAssets();
			AssetFileDescriptor fileDescriptor = null;
			fileDescriptor = assMg.openFd(path);
			mediaplayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						RobotDebug.d(NAME, "播放提示音结束.");
					}
			});
			mediaplayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),fileDescriptor.getLength());
			mediaplayer.prepare();
			fileDescriptor.close();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(mediaplayer == null)
			return ;
		AudioManager mAudioManager = (AudioManager) Robot.getInstance().getContext().getSystemService(Context.AUDIO_SERVICE);
		int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
		mediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaplayer.start();	
		try {
			Thread.sleep(mediaplayer.getDuration() + 100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
