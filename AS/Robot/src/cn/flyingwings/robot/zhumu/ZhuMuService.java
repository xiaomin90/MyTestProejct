package cn.flyingwings.robot.zhumu;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.service.RobotService;
import cn.flyingwings.robot.service.WakeupService;
import cn.flyingwings.robot.xmppservice.XmppService;
import com.android.zhumu.MeetOptions;
import com.android.zhumu.MeetServiceListener;
import com.android.zhumu.ZhuMuSDK;
import com.android.zhumu.ZhuMuSDKInitializeListener;
import com.android.zhumu.ZhuMuStatus;
import com.android.zhumu.ZhuMuStatus.MeetingStatus;

/**
 * 创建瞩目音视频服务
 * @author min.js
 *
 */
public class ZhuMuService extends RobotService implements  Constants, MeetServiceListener,ZhuMuSDKInitializeListener{

	public static final String NAME = "zhumu";
	public static int   status =  MeetingStatus.MEETING_STATUS_IDLE;
	public static final String result = "{\"opcode\":\"92902\",\"status\":\"%s\",\"video\":%d,\"audio\":%d}";
	public enum  ZHUMUMeetingType{
		AUDIO,VIDEO_AUIDO,VIEW
	}
	public MeetingDect meetingDect = MeetingDect.getInstance();
	public static ZHUMUMeetingType meetingtype = null; 
	public static boolean isMeetingSDKInitOK = false;
	
	//-------------数据统计-----------------------// 
	public static String  temp_roomid  = null;
	public static String  reason = null;
	//-------------数据统计 ----------------------//
	
	
	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public void start() {
		super.start();
		// 初始化瞩目相关sdk
		ZhuMuSDK zhuMuSDK = ZhuMuSDK.getInstance();
		zhuMuSDK.initSDK(Robot.getInstance().getContext(),this);
		zhuMuSDK.setDropBoxAppKeyPair(Robot.getInstance().getContext());
		zhuMuSDK.setOneDriveClientId(Robot.getInstance().getContext(), null);
		if (zhuMuSDK.isInitialized()) {
				zhuMuSDK.addListener(this);
		}
	}

	@Override
	public void stop() {
		super.stop();
		ZhuMuSDK zhuMuSDK = ZhuMuSDK.getInstance();
		if (zhuMuSDK.isInitialized()) {
			zhuMuSDK.removeListener(this);
		}
		meetingDect.destory();
	}

	/**
	 * 加入房间，建立音视频连接
	 * @param roomid
	 * @param passwd
	 * @return
	 */
	public boolean joinRoom(String roomid , String passwd, ZHUMUMeetingType type) {
		if(type != null) {
			meetingtype = type;
			//---------------通知服务器 进入会议---------------------------------//
			reason = null;
			temp_roomid = roomid;
			String chat_type = null;
			if(type == ZHUMUMeetingType.AUDIO) {
				chat_type = "audio";
			} else if (type == ZHUMUMeetingType.VIEW) {
				chat_type = "video";
			} else {
				chat_type = "both";
			}
			DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
		    try {
				service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
								.put("function_code", "video_chat").put("function_status", "start")
								.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
								.put("zhumu_pmi",roomid)
								.put("phone_no", XmppService.phone_Number)
								.put("chat_type",chat_type).toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			//---------------通知服务器 进入会议--------------------------------//
		}
		MeetOptions opts = new MeetOptions();
		opts.no_driving_mode = true;
		opts.no_invite = true;
		opts.no_meeting_end_message = true;
//		opts.no_titlebar = true;
//		opts.no_bottom_toolbar = true;
		opts.no_dial_in_via_phone = true;
		opts.no_dial_out_to_phone = true;
		opts.no_disconnect_audio = true;
		opts.no_share = true;
		temp_roomid =  roomid;
		ZhuMuSDK zhuMuSDK = ZhuMuSDK.getInstance();
		if (zhuMuSDK.isInitialized()) {
			RobotDebug.d(NAME, " roomid : " + roomid + " password :" + passwd );
			int ret = zhuMuSDK.joinMeeting(Robot.getInstance().getContext(), roomid,"维拉", passwd,opts);
			this.startDectMeeting();
			RobotDebug.d(NAME, "join meeting ret : " + ret);
			zhuMuSDK.addListener(this);
		}
		return false;
	}
	
	/**
	 * 离开房间，断开音视频连接
	 * @return
	 */
	public boolean leaveRoom() {
		RobotDebug.d(NAME, "leave room send boardcast");
		reason =  "app_end";
		stopDectMeeting();
		Robot.getInstance().getContext().sendBroadcast(new Intent(RobotMeetingActivity.action));
	//	ZhuMuSDK.getInstance().leaveCurrentMeeting(true);
 		return false;
	}
	
	/**
	 * arg0: 1:已经进入meeting  2:有人加入meeting  0:离开meeting成功
	 */
	@Override
	public void onMeetingEvent(int arg0, int arg1, int arg2) {
		ZhuMuService.status = arg0;
		RobotDebug.d(NAME, "meeting status : " + arg0 + "     " + arg1 +  "    " + arg2 );
		// 离开房间
		if(arg0 == MeetingStatus.MEETING_STATUS_IDLE){
				// audio 资源再次获取
				@SuppressWarnings("deprecation")
				AudioRecord   record = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
		                AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT, 4096);
				RobotDebug.d(NAME,"audio status : " + record.getState());
				RobotDebug.d(NAME,"audio Recording status : " + record.getRecordingState());
				if(record.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
					RobotDebug.d(NAME,"开启唤醒");
					WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
					wakeupService.startWakeup();
				} else {
					RobotDebug.d(NAME,"audio Recording status   : " + record.getRecordingState() + " 异常，无法开启唤醒功能");
				}
				record.release();
				record = null;
				// 通知客户端会议结束 
				if(meetingtype != null ) {
					//回复客户端已经关闭会议
					XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
					xmpp.sendMsg("R2C", String.format(result,"success",2,2), 92902);
					//---------------通知服务器 进入会议---------------------------------//
					String chat_type = null;
					if(meetingtype == ZHUMUMeetingType.AUDIO) {
						chat_type = "audio";
					} else if (meetingtype == ZHUMUMeetingType.VIEW) {
						chat_type = "video";
					} else {
						chat_type = "both";
					}
					DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
				    try {
						service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
										.put("function_code", "video_chat").put("function_status", "end")
										.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
										.put("zhumu_pmi",temp_roomid)
										.put("phone_no", XmppService.phone_Number)
										.put("end_reason", reason==null?"others":"app_end")
										.put("chat_type",chat_type).toString());
					} catch (JSONException e) {
						e.printStackTrace();
					}
					//---------------通知服务器 进入会议--------------------------------//
				} else {
					RobotDebug.d(NAME, "会议类型  未知，无法回复客户端");
				}
		}
	}

	@Override
	public void onZhuMuSDKInitializeResult(int errorCode, int internalErrorCode) {
		RobotDebug.d(NAME, "onZhuMuSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
		if(errorCode != ZhuMuStatus.ZMError.ZM_ERROR_SUCCESS) {
			isMeetingSDKInitOK = false;
		} else {
			isMeetingSDKInitOK = true;
		}
	}
	
	/**
	 * 开始会议检测，40s内为收到app进入会议信息，结束当前会议
	 */
	public void startDectMeeting() {
		meetingDect.setStartmeeting();
	}
	
	/**
	 * 停止会议检测，app端已经发送了进入会议的协议
	 */
	public void stopDectMeeting() {
		meetingDect.setMeetingSuccess();
	}
	
}
