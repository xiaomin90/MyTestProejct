package cn.flyingwings.robot.doing.action;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.SystemClock;
import android.text.StaticLayout;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.CANService.CANMessage;
import cn.flyingwings.robot.service.RobotStatusInfoService;

/**
 * 关机任务
 * @author min.js
 */
public class PowerOff extends RobotAction {
	
	public static final String NAME = "power_off";
	private int reason = -1;
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
		 super.parse(s);
		 try {
			JSONObject json = new JSONObject(s);
			if(!json.isNull("reason"))
			{
				reason = json.getInt("reason");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return true;
	}

	@Override
	public void doing() {
		super.doing();
		
		RobotStatusInfoService robotStatusInfo = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
		ChatService chatService = (ChatService)(Robot.getInstance().getService(ChatService.NAME));
		MediaPlayer mediaplayer = null;
		if(null == robotStatusInfo || chatService == null){
			return;
		}
		if((reason != -1) && (reason <= 3)){
			if(!robotStatusInfo.isDndMode()){
				chatService.ttsControll("维拉电量过低即将关机");
			}
		}else if((reason != -1) && (reason > 3)){
			try{
				mediaplayer = new MediaPlayer();
				Context  context = Robot.getInstance().getContext();
				AssetManager assMg = context.getAssets();
				AssetFileDescriptor fileDescriptor = null;
				String musicpath = "robot_record/power_off.mp3";
				fileDescriptor = assMg.openFd(musicpath);
				mediaplayer.setOnCompletionListener(new OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							RobotDebug.d(NAME, "播放结束power_off提示音结束.");
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
			mediaplayer.start();	
		}
		//----------- 通知服务器关机 ----原因------start-----------// 
		DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
		try {
			service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
					.put("function_code", "startup_shutdown").put("function_status", "end")
					.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
					.put("end_reason", reason > 3 ?"by_people":"low_battery").toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//----------- 通知服务器关机 ----原因------end-----------// 
		
		// wait for prompt finished
		SystemClock.sleep(1000 * 5);
		
		CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);

		if (canservice != null) {
			byte msg[] = new byte[1];
			msg[0] = (byte) 0xAA;
			
			if (canservice.send(new CANMessage(0x640, 0x03, msg))) {
				RobotDebug.d(NAME, "Send PowerOff msg success. and stop all ");
				Robot.getInstance().stop();				
				// because all about Robot is stopped, nothing can be dealt correctly, we must wait for power is cut.
				SystemClock.sleep(1000 * 10);
			} else {
				RobotDebug.d(NAME, "Send PowerOff msg failed.");
			}

		}
	}
}
