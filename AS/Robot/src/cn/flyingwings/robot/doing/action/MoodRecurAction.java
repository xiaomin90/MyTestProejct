package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.doing.affair.MoodRecurAffair;

public class MoodRecurAction extends RobotAction {

	public static final String NAME = "mood_recur";
	public String opcode;
	public String opt;
	public JSONObject mood;
	public boolean isVoice = false;
	public String moodName;
	public int pathid = -1;
	

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
		JSONObject datas;
		try {
			datas = new JSONObject(s);
			opcode = datas.getString("opcode");
			opt = datas.optString("opt");
			mood = datas.optJSONObject("mood");
			isVoice = datas.optBoolean("isVoice");
			
			if(!isVoice && mood != null){
				moodName = mood.optString("mood_name");
				pathid = mood.optInt("path_id");
			}else {
				moodName = datas.optString("mood_name");
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return true;
	}

}
