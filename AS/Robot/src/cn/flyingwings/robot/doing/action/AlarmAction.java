package cn.flyingwings.robot.doing.action;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.service.RobotLightEffectsService;
import cn.flyingwings.robot.service.RobotLightEffectsService.LIGHT_EFFECTS;
import cn.flyingwings.robot.smartdeviceservice.Dev433Service;

/**
 * format {"name":"alarm","content":"报警内容"} for temp
 * @author min.js
 *
 */
public class AlarmAction extends RobotAction{
	
	public static final String NAME = "alarm";
	private String content = null;
	
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
			JSONObject json = new JSONObject(s);
			content = json.getString("content");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	@Override
	public void doing() {
		/**
		 * 通知APP 和 语音播报 以及打电话都在dev433service中被处理了,此处仅仅执行播放警报命令后，在执行播放报警内容
		 */
		RobotDebug.d(NAME, "deal alarm. 1");
		MediaPlayer mediaplayer = null;
		try{
			mediaplayer = new MediaPlayer();
			Context  context = Robot.getInstance().getContext();
			AssetManager assMg = context.getAssets();
			AssetFileDescriptor fileDescriptor = null;
			String musicpath = "sound/robot_alarm.mp3";
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
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		if(content != null) {
			try {
				Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", content).toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		RobotLightEffectsService lighteffectsService = (RobotLightEffectsService) Robot.getInstance().getService(RobotLightEffectsService.NAME);
		if(lighteffectsService != null)
		{
			RobotDebug.d(NAME, "deal alarm. 2");
			if(Dev433Service.ONSercurity)
				lighteffectsService.setLightEffect(LIGHT_EFFECTS.Normal_Work_Status);
			else
				lighteffectsService.setLightEffect(LIGHT_EFFECTS.Sercurity_Off);
		}
	}
}
