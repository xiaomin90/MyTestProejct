package cn.flyingwings.robot.doing.affair;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.motion.MotionControl;
import cn.flyingwings.robot.motion.MoveDect;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.CANService.CANMessage;
import cn.flyingwings.robot.service.RobotStatusInfoService;

public class DanceAffair extends RobotAffair {
	
	public  static final String NAME = "dance";
	private MediaPlayer mediaplayer = null;
	
	public DanceAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		RobotStatusInfoService service = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
		/**
		 * 电量小于10%。不能执行跳舞的命令
		 */
		if(service.getBatteryLevel() < 10) {
			RobotDebug.d(NAME, "机器人正在充电,且电量小于10%。不能执行跳舞的命令");
			/**
			 * say任务与跳舞冲突所以会执行结束事务 
			 **/
			try {
				Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", "维拉电量过低，需要充电补充能量，下次再给您跳舞吧").toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return ;
		}
		
		/**
		 * 在充电座上需要向前走一段距离，然后再跳舞
		 */
		if(service.getBatteryStatus() == 1) {
			// 前进3秒
			byte[] data = new byte[4];
			data[0] = (byte) ((60 >> 8) & 0x00ff);
			data[1] = (byte) (Math.abs((60)) & 0x00ff);
			data[2] = (byte) ((60 >> 8) & 0x00ff);
			data[3] = (byte) (Math.abs(60) & 0x00ff);
			byte[] sendData = new byte[4];
			sendData[0] = data[1];
			sendData[1] = data[3];
			sendData[2] = (byte) ((0 >> 8) & 0x00ff);
			sendData[3] = (byte) (Math.abs((0)) & 0x00ff);
			CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
			CANMessage msg = new CANMessage(0x100, 0x2, sendData);
			canservice.send(msg);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 停下来
			CANMessage headCanMsg = new CANMessage(0x120, 0x0, new byte[0]);
			canservice.send(headCanMsg);
			CANMessage footCanMsg = new CANMessage(0x100, 0x0, new byte[0]);
			canservice.send(footCanMsg);
		}
		ChatService chatService = (ChatService) (Robot.getInstance().getService(ChatService.NAME));
		chatService.ttsControll("开始跳舞");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try{
			mediaplayer = new MediaPlayer();
			Context  context = Robot.getInstance().getContext();
			AssetManager assMg = context.getAssets();
			AssetFileDescriptor fileDescriptor = null;
			String musicpath = "music/happy.mp3";
			fileDescriptor = assMg.openFd(musicpath);
			mediaplayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						stopAffair(true);
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
		CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
		byte[] data = new byte[1];
		data[0] = 1;
		canservice.send(new CANMessage(0x760,0,data));
	}
	
	@Override
	protected void onFinished() {
		super.onFinished();
		byte[] data = new byte[1];
		data[0] = 2;
		for(int i= 0 ;i<3; i++) {
			CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
			if(canservice.send(new CANMessage(0x760,0,data))) {
				RobotDebug.d(NAME, "发送停止跳舞指令成功.");
				break;
			}
			else{
				RobotDebug.d(NAME, "发送停止跳舞指令失败.");
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(mediaplayer != null){
			mediaplayer.release();
			mediaplayer = null;
		}
	}
}
