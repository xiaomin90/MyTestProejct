package cn.flyingwings.robot.doing.affair;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.FaceService.FaceService;
import cn.flyingwings.robot.doing.action.RobotAction;

/**
 * 机器人事务管理器
 * 负责事务的切换和转交任务到当前事务，转发网络消息到当前事务。
 * @author gd.zhang
 *
 */
public class RobotAffairManager {
	private final static String TAG = "RobotAffairManager";
	
	private HashMap<String, Class<RobotAffair>> affairSet = new HashMap<String, Class<RobotAffair>>();
	
	private RobotAffair mAffair, preAffair;
	private boolean started = false;
	// 调用finished() 与 switchAffair()方法必须互斥,这样可以避免对mAffair 、 perAffair 的 操作出现问题
	private Lock lockswAffair = new ReentrantLock();   
	private volatile boolean switchingAffair = false;
	public RobotAffairManager() {
		AllAffair.initAllAffair(this);
	}
	
	/**
	 * 注册一个事务到事务管理器中
	 * @param name 事务管理器名称
	 * @param c 事务管理器的类
	 * @return 是否注册成功，如果已经存在同名事务，则注册失败
	 */
	public boolean addAffair(String name, Class<?> c) {
		if ((name == null) || (c == null))
			return false;
		if (!RobotAffair.class.isAssignableFrom(c))
			return false;
		if (affairSet.containsKey(name))
			return false;
		@SuppressWarnings("unchecked")
		Class<RobotAffair> cc = (Class<RobotAffair>)c;
		affairSet.put(name, cc);
		return true;
	}
	
	/**
	 * 切换到指定的事务
	 * @param name 指定的事务名称
	 * @param act 引起事务切换的任务
	 * @return 是否切换事务成功
	 */
	public boolean switchAffair(String name, RobotAction act) {
		lockswAffair.lock();
		switchingAffair = true;
		boolean successful = switchAffairInternal(name, act);
		if(!successful) {
			startIdleAffair();
		}
		FaceService faceservice = (FaceService) Robot.getInstance().getService(FaceService.NAME);
		if(faceservice != null ) {
			faceservice.clearFaceStack();
		}
		lockswAffair.unlock();
		switchingAffair = false;
		return successful;
	}

	private synchronized boolean switchAffairInternal(String name, RobotAction act) {
		if (!affairSet.containsKey(name)) {
			RobotDebug.d(TAG, "Affair not found "+name);
			return false;
		}
		preAffair = mAffair;
		mAffair = null;
		if(preAffair !=null){
			RobotDebug.d(TAG,"switchAffair  preAffair is " + preAffair.name());
			preAffair.stopAffair(false);
		}
		else{
			RobotDebug.d(TAG,"switchAffair  preAffair is null");
		}
		/**
		 * TODO
		 * 阻塞检测，如果一段时间内当前事务没有结束，那么强制结束它。
		 */
		/* 按照事务名称，获取事务类信息，实例化一个 */
		Class<RobotAffair> cdf = affairSet.get(name);
		if (cdf == null) {
//			startIdleAffair();
			return false;
		}
		try {
			Constructor<RobotAffair> con = cdf.getConstructor(RobotAffairManager.class);
			mAffair = con.newInstance(this);
		} catch (Exception e) {
			RobotDebug.d(TAG, "Can not found constructor in affair "+name);
//			startIdleAffair();
			return false;
		}
		if(mAffair == null) {
			RobotDebug.d(TAG, "create affair instance fail "+name);
//			startIdleAffair();
			return false;
		}
		/* 启动新的事务 */
		mAffair.start();
		mAffair.waitAffairReady();
		RobotDebug.d(TAG, "affair  start  : " + mAffair.name());
		mAffair.startAffair(act);
		return true;
	}
	
	/**
	 * 返回当前事务的名称
	 * @return 当前事务的名称
	 */
	public String currentAffair(){
		if(mAffair != null)
			return mAffair.name();
		else
			return null;
	}
	
	/**
	 * 请求执行一个动作
	 * @param act 动作
	 * @return 是否请求成功
	 */
	public boolean toDo(RobotAction act) {
		if(mAffair != null) {
			mAffair.toDo(act);
			return true;
		}
		else
			return false;
	}
	
	/**
	 * 通知事务管理器，一个事务退出
	 * @param affair 退出的事务
	 */
	public void finished(RobotAffair affair) {
		/*事务管理器正在切换事务，不需要进行通知管理器*/
		if(switchingAffair)
			return;
		lockswAffair.lock();
		RobotDebug.d(TAG, "finish " + affair.name() + " affair" + " preaffair " + (preAffair!=null?preAffair.name():"null"));
		if (affair == preAffair) {
			preAffair = null;
		} else if (affair == mAffair) {
			mAffair = null;
			if (started)
				startIdleAffair();
			FaceService faceservice = (FaceService) Robot.getInstance().getService(FaceService.NAME);
			if(faceservice != null) {
				faceservice.clearFaceStack();
			}
		}
		lockswAffair.unlock();
	}
	
	/**
	 * 开始事务管理器的运行
	 */
	public void start() {
		started = true;
		startIdleAffair();
	}
	
	/**
	 * 停止事务管理器的运行
	 */
	@SuppressWarnings("deprecation")
	public void stopDoing() {
		started = false;
		RobotAffair af = mAffair;
		mAffair.stopAffair(false);
		try {
			af.interrupt();
			af.join(500);
			af.stop();
		} catch (Exception e) {
			RobotDebug.d(TAG, "stop affairmanager exception "+e.getLocalizedMessage());
		}
	}
	
	private synchronized void startIdleAffair() {
		preAffair = null;
		if(mAffair != null) {
			return;
		}
		mAffair = new IdleAffair(this);
		mAffair.start();
		mAffair.waitAffairReady();
		RobotDebug.d(TAG, "affair  start  idle");
		mAffair.startAffair(null);
		
	}
}
