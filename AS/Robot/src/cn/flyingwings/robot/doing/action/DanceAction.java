package cn.flyingwings.robot.doing.action;

/**
 * 跳舞任务执行
 * @author js.min
 *
 */
public class DanceAction  extends RobotAction{
	public static final String NAME = "dance";
	
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
		return super.parse(s);
	}

	@Override
	public void doing() {
		super.doing();
	}

	
}
