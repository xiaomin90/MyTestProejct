package cn.flyingwings.robot;

import android.content.Context;
import android.content.IntentFilter;
import cn.flyingwings.robot.doing.RobotManager;
import cn.flyingwings.robot.motion.MoveDect;
import cn.flyingwings.robot.service.RobotService;
import cn.flyingwings.robot.service.ServiceManager;

/**
 * 机器人系统<br>
 * @author gd.zhang
 *
 */
public class Robot {
	
	private static Robot mRobot = null;
	private RobotManager mManager;
	private ServiceManager mService;
	private Context context = null;
	
	private Robot(){}
	
	public static Robot getInstance() {
		if(mRobot == null) {
			mRobot = new Robot();
		}
		return mRobot;
	}
	
	/**
	 * 开启机器人系统
	 * @param c 运行上下文，用于注册事件广播接收
	 */
	public void start(Context c) {
		context = c;
		mManager = new RobotManager();
		mService = new ServiceManager();
		mService.initAllService();
		mManager.startDoing();

		IntentFilter intentFilter = new IntentFilter(RobotManager.EVENT_BROADCAST);
		context.registerReceiver(mManager, intentFilter);
	}
	
	/**
	 * 停止机器人系统
	 */
	public void stop() {
		context.unregisterReceiver(mManager);
		mManager.stopDoing();
		mService.stopAllService();
		MoveDect.getInstance().destory();
	}
	
	/**
	 * 获取任务调度器
	 * @return 任务调度器，任务调度器没有启动时返回{@code null}。
	 */
	public RobotManager getManager() {
		return mManager;
	}
	
	/**
	 * 获取一个服务
	 * @param name 服务名称，找不到该服务时返回 {@code null}.
	 * @return
	 */
	public RobotService getService(String name) {
		return mService.getService(name);
	}
	
	/**
	 * 添加一个服务
	 */
	public boolean startService(RobotService s)
	{
		return mService.startService(s);
	}
	
	
	/**
	 * 获取当前机器人系统使用的{@link Context}，用于进行系统操作
	 * @return 当前可用的 {@link Context}，或 {@code null}
	 */
	public Context getContext() {
		return context;
	}
	
	static {
		System.loadLibrary("Robot");
	}
}
