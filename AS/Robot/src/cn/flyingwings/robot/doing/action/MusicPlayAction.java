package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.service.MusicPlayService;

public class MusicPlayAction extends RobotAction {

	public static final String NAME = "music_play";
	public static final String TAG = "MusicPlayAction";

	public String songName;
	public String artist;
	public String opt;
	public String opcode;
	public boolean keepPlay = false;

	@Override
	public int type() {
		// TODO Auto-generated method stub
		return RobotAction.ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	public void doing() {
		// TODO Auto-generated method stub
		super.doing();
		if (!TextUtils.isEmpty(opcode)) {
			Log.i(TAG, "this action from app. opcode:" + opcode + "  opt:" + opt);
			Context context = Robot.getInstance().getContext();
			Intent intent = new Intent(MusicPlayService.MUSIC_ACTION);
			intent.putExtra("opcode", opcode);
			if (!TextUtils.isEmpty(opt)) {
				intent.putExtra("opt", opt);
			}
			context.sendBroadcast(intent);
		} else {
			MusicPlayService musicPlayService = (MusicPlayService) Robot.getInstance()
					.getService(MusicPlayService.NAME);
			if(keepPlay){
				musicPlayService.keepPlay();
			}else{
				musicPlayService.playMusic(songName, artist);

				if (!TextUtils.isEmpty(songName)) {
					MusicPlayService.loopPlay = false;
				} else {
					MusicPlayService.loopPlay = true;
					if (!TextUtils.isEmpty(artist))
						MusicPlayService.loopArtist = artist;
				}
			}
			
			

		}

	}

	@Override
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		super.parse(s);
		try {
			JSONObject datas = new JSONObject(s);
			songName = datas.optString("songName");
			artist = datas.optString("artist");
			opt = datas.optString("opt");
			keepPlay = datas.optBoolean("keep_play");
			
			if (!opt.equals("stop")) {
				MusicPlayService.setMusicFlag(true);
			} else {
				MusicPlayService.setMusicFlag(false);
			}
			opcode = datas.optString("opcode");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

}
