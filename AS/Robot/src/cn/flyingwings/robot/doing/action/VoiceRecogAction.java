package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.chatService.ChatDebug;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.ChatService.ControllEnum;
import cn.flyingwings.robot.chatService.SpeechRecognize;

public class VoiceRecogAction extends RobotAction {

	public static final String NAME = "voice_recognize";
	private String subName = "";

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

		if (TextUtils.isEmpty(subName)) {
			ChatService chatService = (ChatService) Robot.getInstance().getService(ChatService.NAME);
			chatService.ttsStop();
			ChatDebug.i("RECOGNIZ_STATUS", "call VoiceRecogAction.doing() setRecogStatus(SpeechRecognize.END)");
		    //chatService.asrControll(ControllEnum.pause);
			chatService.asrControll(ControllEnum.start);
		}

	}

	@Override
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		try {
			JSONObject mObject = new JSONObject(s);
			subName = mObject.optString("subName");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.parse(s);
	}

	
}
