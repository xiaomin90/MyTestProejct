package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.affair.WizardAffair;

/**
 * 开机向导
 * @author min.js
 * json fomart  {"name":"wizard","activated":true}
 * activated  false  表示没有激活
 * activated  true   表示已经激活
 */
public class WizardAction extends RobotAction {
	
	public  static String NAME = "wizard";
	private boolean isactivated = false;
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
		isactivated = false;
		try {
			JSONObject datas = new JSONObject(s);
			isactivated = datas.getBoolean("activated");
		} catch (JSONException e) {
			RobotDebug.d(NAME, "parse not json format e: " + e.toString());
		}
		return super.parse(s);
	}

	@Override
	public void doing() {
		super.doing();
		Intent intent = new Intent(WizardAffair.BoardCastIntent);
		intent.putExtra("activated", isactivated);
		Robot.getInstance().getContext().sendBroadcast(intent);
	}
	
	
}
