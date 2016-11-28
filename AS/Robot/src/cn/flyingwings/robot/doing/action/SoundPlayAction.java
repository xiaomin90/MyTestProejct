package cn.flyingwings.robot.doing.action;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.text.TextUtils;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.FaceService.FaceService;
import cn.flyingwings.robot.FaceService.FaceType;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.chatService.SpeechRecognize;
import cn.flyingwings.robot.chatService.ModuleController.State;
import cn.flyingwings.robot.xmppservice.XmppService;

public class SoundPlayAction extends RobotAction {

	public static final String NAME = "sound_play";
	public String res;
	public boolean isRecord = false;
	private static MediaPlayer mediaPlayer;
	private String subname = null;
	private String musicname = null;
	private static boolean isInteral = false;

	@Override
	public int type() {
		// TODO Auto-generated method stub
		return ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	// {"name": "sound_play", "res": "resName"}

	@Override
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		subname = null;
		musicname = null;
		try {
			JSONObject jsonData = new JSONObject(s);
			res = jsonData.optString("res");
			musicname = res;
			isRecord = jsonData.optBoolean("isRecord");
			if (!isRecord) {
				if (res.equals("wake_up")) {
					res = String.format("sound/%s.wav", res);
				} else {
					res = String.format("sound/%s.mp3", res);
				}

			} else {
				res = String.format("robot_record/%s.mp3", res);
			}
			/* 特殊的app播放发布会的需求 */
			if (!jsonData.isNull("subname")) {
				subname = jsonData.getString("subname");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.parse(s);
		return true;
	}

	@Override
	public void doing() {
		// TODO Auto-generated method stub
		super.doing();
		
		final Context context = Robot.getInstance().getContext();
		AssetManager assMg = context.getAssets();
		AssetFileDescriptor fileDescriptor = null;
		try {
			try {
				if (TextUtils.isEmpty(res)) {
					if (subname != null) {
						XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
						xmppservice.sendMsg("R2C", new JSONObject().put("opcode", "98001").put("music_name", musicname)
								.put("result", 1).put("error_msg", "音乐不存在").toString(), 98001);
					}
					return;
				}
				if (subname != null) {
					XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
					xmppservice.sendMsg("R2C", new JSONObject().put("opcode", "98001").put("music_name", musicname)
							.put("result", 0).toString(), 98001);
				}
			} catch (JSONException e) {
				RobotDebug.d(NAME, "replay app play music. json format e: " + e.getMessage());
			}
			fileDescriptor = assMg.openFd(res);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {
					onFinished();
					
				}
			});
			mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),
					fileDescriptor.getLength());
			mediaPlayer.prepare();
			fileDescriptor.close();

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (mediaPlayer == null)
			return;
		mediaPlayer.start();
		
		while(mediaPlayer != null && mediaPlayer.isPlaying()){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		ModuleController.getInstance().changeState(State.IDLE, null);

	}

	private static void onFinished() {
		// TODO Auto-generated method stub
		// handlerThread.quit();
		Log.i("sound_play", "sound_play action finish");
		if (mediaPlayer != null) {
		    if(mediaPlayer.isPlaying()){
		    	mediaPlayer.pause();
		    }
			mediaPlayer.reset();
			mediaPlayer.release();
			mediaPlayer = null;
		}
        
		
	}
	
	

	public static void cancel(){
		onFinished();
	}

}
