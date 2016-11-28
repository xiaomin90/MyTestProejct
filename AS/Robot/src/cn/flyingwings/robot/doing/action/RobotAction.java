package cn.flyingwings.robot.doing.action;

import cn.flyingwings.robot.FaceService.FaceService;

/**
 * 动作，初始化时填充 {@link #name} 和 {@link #type} 字段<br>
 * 动作是完成一个基础功能的单元，不能阻塞。重载此类的 {@link #doing()} 实现各种不同的动作，重载 {@link #parse(String)} 实现参数解析。
 * @author gd.zhang
 *
 */
public abstract class RobotAction {
	
	public final static int ACTION_TYPE_SIMPLE = 0x01;
	public final static int ACTION_TYPE_TASK = 0x02;
	public final static int ACTION_TYPE_VIRTUAL = 0x03;
	private String actionstr = null;
	public abstract int type();
	
	public abstract String name();
	
	/**
	 * 重载这个函数实现动作的具体功能
	 */
	public void doing() {
	}
	
	/**
	 * 参数解析函数
	 * 重载这个函数实现参数解析
	 * @param s 动作请求字符串
	 * @return 是否解析成功
	 */
	public boolean parse(String s) {
		actionstr = s;
		return true;
	}
	
	/**
	 * 切换界面
	 * 不需要继承该接口
	 */
	public void changeFace() {
		FaceService.parse(actionstr);
	}
}
