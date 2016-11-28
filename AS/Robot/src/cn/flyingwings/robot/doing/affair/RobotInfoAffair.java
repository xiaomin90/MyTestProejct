package cn.flyingwings.robot.doing.affair;

public class RobotInfoAffair extends RobotAffair {
	
	public static final String NAME = "robot_info";

	public RobotInfoAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}

}
