package cn.flyingwings.robot.doing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cn.flyingwings.robot.doing.action.RobotDivide;
import cn.flyingwings.robot.doing.affair.RobotAffairManager;
import cn.flyingwings.robot.doing.event.RobotEventMap;


/**
 * 机器人系统的任务调度器。
 * 负责所有事件的响应，任务的处理和事务的切换，外部所有的服务理论上只要调用该类提供的接口已经足够使用。
 * 实现了广播接收类，接收系统事件，创建该类时应该将该类注册一个广播，广播名称{@link #EVENT_BROADCAST}。
 * 
 * @author gd.zhang
 *
 */
public class RobotManager extends BroadcastReceiver {
	
	public static final String EVENT_BROADCAST = "cn.flyingwings.robot.event_happened";
	
	private RobotEventMap eventMapper;
	private RobotDivide divider;
	private RobotActuate actuator;
	private RobotAffairManager affairManager;
	
	private boolean started = false;
	
	public RobotManager() {
		affairManager = new RobotAffairManager();
		actuator = new RobotActuate();
		divider = new RobotDivide(actuator, affairManager);
		eventMapper = new RobotEventMap(divider);
	}
	
	/**
	 * 所有的任务都已经启动，通知任务调度器开始工作，之前的所有事件和动作请求都会被忽略。
	 */
	public void startDoing() {
		started = true;
		affairManager.start();
		actuator.start();
		divider.start();
		eventMapper.start();
	}
	
	/**
	 * 停止事务管理器运行
	 * 停止各个模块。此后的所有事件和动作请求都将被忽略。
	 */
	public void stopDoing() {
		started = false;
		eventMapper.stopDoing();
		divider.stopDoing();
		actuator.stopDoing();
		affairManager.stopDoing();
	}
	
	/**
	 * 提交一个事件
	 * @param intent 事件内容，{@link Intent}内部的 Action 将被忽略
	 */
	public void sendEvent(Intent intent) {
		if (started)
			eventMapper.mapEvent(intent);
	}
	
	/**
	 * 提交一个动作请求
	 * @param a json格式的动作请求，
	 */
	public void toDo(String a) {
		if(started)
			divider.toDo(a);
	}
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(started)
			eventMapper.mapEvent(intent);
	}
	
	/*
	 * 获取当前事务名称
	 * */
	public String getCurrentAffair(){
		return affairManager.currentAffair();
	}
	
	
}
