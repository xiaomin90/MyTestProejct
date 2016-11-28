package cn.flyingwings.robot.doing.affair;

import android.util.Log;
import cn.flyingwings.robot.doing.action.RobotAction;

public class ClientControl extends RobotAffair {


	public static final String NAME = "client_control";
	public static final String TAG = "client_control";
	
	@Override
	public String name() {
		return NAME;
	}


	public ClientControl(RobotAffairManager am) {
		super(am);
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		Log.d(NAME, "start affair "+NAME);
	}
	
	@Override
	protected void onFinished() {
		Log.d(TAG, "finish affair "+NAME);
	}
}
