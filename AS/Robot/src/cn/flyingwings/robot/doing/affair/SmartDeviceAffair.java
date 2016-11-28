package cn.flyingwings.robot.doing.affair;

import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotAction;

public class SmartDeviceAffair extends RobotAffair {

	public static String NAME = "smart_device";
	public SmartDeviceAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		RobotDebug.d(NAME,"smart_device Affair Start ");
	}
	
	@Override
	protected void onFinished() {
		RobotDebug.d(NAME, "smart_device Affair is finished.");
	}
}
