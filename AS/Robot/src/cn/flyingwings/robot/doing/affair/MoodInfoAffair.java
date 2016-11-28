package cn.flyingwings.robot.doing.affair;

import cn.flyingwings.robot.doing.action.RobotAction;

public class MoodInfoAffair extends RobotAffair{
	public static final String NAME = "mood_info";
	public MoodInfoAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
	}

	@Override
	protected void onFinished() {
		super.onFinished();
	}

}
