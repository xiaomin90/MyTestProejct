package cn.flyingwings.robot.doing.affair;

import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotAction;

public class AlarmAffair  extends RobotAffair {
	
	public static final String NAME = "alarm";
	
	public AlarmAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		RobotDebug.d(NAME,"alarmAffair Start ");
	}
	 
	@Override
	protected void onFinished() {
		RobotDebug.d(NAME,"alarmAffair onfinish");
	}

}
