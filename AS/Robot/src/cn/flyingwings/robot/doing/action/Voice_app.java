package cn.flyingwings.robot.doing.action;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotServerURL;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.RobotServerURL.HttpMethod;
import cn.flyingwings.robot.RobotServerURL.HttpUrl;
import cn.flyingwings.robot.chatService.TuRingUtils.WebCallBack;
import cn.flyingwings.robot.chatService.BehaviorsBean;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.TuRingUtils;
import cn.flyingwings.robot.chatService.TuringDo;
import cn.flyingwings.robot.xmppservice.XmppService;

public class Voice_app extends RobotAction {

	public static final String NAME = "voice_app";
	private String content = "";

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
		RobotDebug.d(NAME, "doing :" + content);
		TuringDo turingDo = new TuringDo(Robot.getInstance().getContext());
		turingDo.postOrder(content, callback);
	}

	private WebCallBack callback = new WebCallBack() {

		@Override
		public void onSuccess(Object... args) {
			// TODO Auto-generated method stub
			JSONObject obj = null;
			String response = "";
			try {
				obj = TuRingUtils.behaviorAnalysis((BehaviorsBean) args[3], (String) args[4], TuRingUtils.FROM_APP);
				Map<String, String> params = new HashMap<String, String>();
				params.put("speech_txt", (String)(args[4]));
				params.put("answer_txt", (String)(args[2]));
				params.put("session_id",  XmppService.conSSessionID);
				params.put("version", RobotVersion.current_system_sw_version);
				params.put("speech_way", "APP");
				RobotServerURL.doRequest(HttpUrl.voiceLogDo, params, HttpMethod.post, null, null);
				if (obj != null) {
					ChatService chatService = (ChatService) Robot.getInstance().getService(ChatService.NAME);
					if (obj.optInt("type") == 0x1002) {
						response = obj.optJSONObject("datas").optString("content");
						chatService.ttsControll(response);
					} else {
						response = chatService.orderDispath(obj, false);
					}

				}
				xmppPost(response);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onError() {
			// TODO Auto-generated method stub

		}
	};

	private void xmppPost(String response) {
		XmppService service = (XmppService) Robot.getInstance().getService(XmppService.NAME);
		JSONObject obj = new JSONObject();
		try {
			obj.put("opcode", "92801");
			obj.put("content", content);
			obj.put("reply", response);
			obj.put("result", 0);
			service.sendMsg("R2C", obj.toString(), 92801);
		} catch (JSONException e) {
			RobotDebug.d(NAME, "doing :" + content + " e:" + e.toString());
		}
	}
}
