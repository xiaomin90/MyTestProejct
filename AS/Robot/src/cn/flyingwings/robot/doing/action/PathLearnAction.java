package cn.flyingwings.robot.doing.action;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.doing.affair.PathLearnAffair;
import cn.flyingwings.robot.xmppservice.TransferCode;

/**
 * 路径学习类
 * 
 * @author min.js
 * 
 *         {"name":"path_learn","opcode":xxx,"opt":"xxx","path":{xxx}}
 *
 *         opcode
 *
 *         opcode opt path
 */
public class PathLearnAction extends RobotAction {

	public static final String NAME = "path_learn";
	public String opcode = "";
	public String opt = null;
	public JSONObject path = null;
	public int path_id = -1;
	public String path_name = null;
	

	@Override
	public int type() {
		return RobotAction.ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public boolean parse(String s) {
		super.parse(s);
		Log.i(NAME, s);
		try {
			JSONObject datas = new JSONObject(s);
			opcode = datas.getString("opcode");
			if (opcode.equals("93101")) {
				opt = datas.getString("opt");
				path = datas.getJSONObject("path");
				path_name = path.getString("name");
				path_id = path.optInt("id");
			}
			if (TextUtils.isEmpty(opcode))
				return false; 
			optDispatch();
		} catch (JSONException e) {
			RobotDebug.d(NAME, "pathlearn  format  e : " + e.toString());
		}
		return true;
	}

	@Override
	public void doing() {
		super.doing();
	}
	
	private void optDispatch() {
		PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		if (TextUtils.isEmpty(opt)) {
			pathService.getAllPath();
		} else if (opt.equals(PathLearnAffair.START_STUDY)) {
			// 开始路径学习
			pathService.startLearn(path_name);
		} else if (opt.equals(PathLearnAffair.CANCEL_STUDY)) {
			// 取消路径学习
			pathService.cancelLearn();
		} else if (opt.equals(PathLearnAffair.FINISH_STUDY)) {
			// 完成路径学习
			pathService.learnEnd();
		} else if (opt.equals(PathLearnAffair.UPDATE_PATH_NAME)) {
			// 修改路径名称
			pathService.updatePathName(path_id, path_name);
		} else if (opt.equals(PathLearnAffair.DEL_PATH)) {
			// 删除路径
			pathService.deletePath(path_id, path_name);
		}
	}
	

}
