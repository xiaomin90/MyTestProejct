package cn.flyingwings.robot.doing.event;

import cn.flyingwings.robot.doing.action.RobotDivide;
import android.content.Intent;

/**
 * 事件映射项，初始化时填充 {@link #name} 字段<br>
 * 用来将一个指定事件映射为动作请求。实现此类的 {@link #map(RobotDivide, Intent)} 接口完成事件的映射，并提交到动作解析器。
 * @author gd.zhang
 *
 */
public abstract class RobotEvent {
	
	/**
	 * 事件名称，重载时返回事件名称。
	 */
	public abstract String name();
	
	/**
	 * 映射函数体，将一个事件映射为动作请求，解析其中的参数，提交给动作解析器
	 * @param divider 动作解析器的引用
	 * @param intent 事件内容
	 */
	abstract public void map(RobotDivide divider, Intent intent); 
}
