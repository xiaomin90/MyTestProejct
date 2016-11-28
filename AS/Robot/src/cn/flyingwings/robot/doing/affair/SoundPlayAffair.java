package cn.flyingwings.robot.doing.affair;

import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.doing.action.SoundPlayAction;

public class SoundPlayAffair extends RobotAffair {

	public static final String NAME = "sound_play";

	public SoundPlayAffair(RobotAffairManager am) {
		super(am);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	protected void onCreated(RobotAction act) {
		// TODO Auto-generated method stub
		super.onCreated(act);

	}
	

	@Override
	protected void onAction(RobotAction act) {
		// TODO Auto-generated method stub
		super.onAction(act);
	}

	@Override
	protected void onFinished() {
		// TODO Auto-generated method stub
		super.onFinished();
	}

}
