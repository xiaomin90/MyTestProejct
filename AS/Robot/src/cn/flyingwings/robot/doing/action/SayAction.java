package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;


import android.text.TextUtils;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.ChatService.ControllEnum;
import cn.flyingwings.robot.doing.affair.MusicAffair;
import cn.flyingwings.robot.service.MusicPlayService;

public class SayAction extends RobotAction {

	public static final String NAME = "say";
	private String content = "";

	/*
	 * {"name":"say","content":"xxx"}
	 */
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
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		try {
			JSONObject mDatas = new JSONObject(s);
			content = mDatas.optString("content");
			RobotDebug.d(NAME, "parse :" + content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.parse(s);
	}

	@Override
	public void doing() {
		// TODO Auto-generated method stub
		super.doing();
		Log.e("voice_module", "sayAction doing");
		RobotDebug.d(NAME, "doing :" + content);
		if (!TextUtils.isEmpty(content)) {
			ChatService chatService = (ChatService) (Robot.getInstance().getService(ChatService.NAME));
			chatService.ttsControll(content);
		}

	}
}
