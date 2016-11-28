package cn.flyingwings.robot.doing.affair;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.doing.action.RobotAction;

public class PathLearnAffair extends RobotAffair {
	public static final String NAME = "path_learn";
	public static final String START_STUDY = "start_study";
	public static final String CANCEL_STUDY = "cancel_study";
	public static final String FINISH_STUDY = "finish_study";
	public static final String UPDATE_PATH_NAME = "update_path_name";
	public static final String DEL_PATH = "del_path";
	public PathLearnAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
	}
	
	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onFinished() {
		super.onFinished();
		PathService mPathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		if(mPathService.isInLearning()){
			mPathService.cancelLearn();
		}
	}
	
}
