package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.xmppservice.TransferCode;

public class MoodinfoAction extends RobotAction {

	public static final String NAME = "mood_info";
	public String opcode;
	public String opt;
	public JSONObject mood;
	public int index;

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
		
	}

	@Override
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		super.parse(s);
		try {
			JSONObject datas = new JSONObject(s);
			opcode = datas.optString("opcode");
			opt = datas.optString("opt");
			mood = datas.optJSONObject("mood");
			index = datas.optInt("index");
			if (TextUtils.isEmpty(opcode))
					return false;
			int code = Integer.valueOf(opcode);
			switch (code) {
				case TransferCode.robot_mood_add_del_update:
					optDispatch(opt, mood);
					break;
				case TransferCode.robot_mood_inquire:
					PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
					pathService.getMoods(index);
					break;
				default:
					break;
				}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void optDispatch(String opt, JSONObject mood) throws JSONException {
		PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		if (TextUtils.isEmpty(opt) || mood == null) {
				return;
		}
		if (opt.equals("add")) {
			pathService.addMoodInfo(mood);
		} else if (opt.equals("update")) {
			pathService.updateMoodName(mood);
		} else if (opt.equals("del")) {
			pathService.delMoodInfo(mood);
		}
	}
	
}
