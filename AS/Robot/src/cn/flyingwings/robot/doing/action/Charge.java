package cn.flyingwings.robot.doing.action;


public class Charge  extends RobotAction {

	public static String NAME = "charge";
	private String TAGS = "Charge";
	
	
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
		//....
		super.parse(s);
		return true;
	}
	
	@Override
	public void doing() {
			//...
	}
	
	
	
}
