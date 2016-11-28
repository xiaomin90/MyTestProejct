package cn.flyingwings.robot.doing.affair;
import cn.flyingwings.robot.doing.action.RobotAction;

/**
 * 关机事务
 * @author min.js
 *
 */
public class PowerOffAffair extends RobotAffair{

	public static final String NAME = "power_off";
	
	public PowerOffAffair(RobotAffairManager am) {
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
