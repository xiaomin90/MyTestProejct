package cn.flyingwings.robot.doing.affair;

import android.util.Log;
import cn.flyingwings.robot.doing.action.RobotAction;

public class UpdateAffair extends RobotAffair {

	public static String NAME = "update";
	
	public UpdateAffair(RobotAffairManager am) {
		super(am);	
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		Log.d(NAME,"updateAffair Start ");
	}
	
	@Override
	protected void onFinished() {
		Log.d(NAME, "updateAffair is finished.");
	}
	
}
