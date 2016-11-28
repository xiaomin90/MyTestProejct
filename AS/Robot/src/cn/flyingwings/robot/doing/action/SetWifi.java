package cn.flyingwings.robot.doing.action;

public class SetWifi extends RobotAction {

	public static final String NAME = "set_wifi";
	
	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int type() {
		return ACTION_TYPE_VIRTUAL;
	}
	
	

}
