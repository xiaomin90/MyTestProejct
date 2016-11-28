package cn.flyingwings.robot.doing.event;

/**
 * 事件注册类<br>
 * 将所有的事件在这个类中统一注册，方便管理。
 * @author gd.zhang
 *
 */
public class AllEvent {
	public static void initAllEvent(RobotEventMap map) {
		//map.addEvent(new TestEvent());
		map.addEvent(new ClientChange());
	}
}
