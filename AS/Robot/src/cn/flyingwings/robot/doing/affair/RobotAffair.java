package cn.flyingwings.robot.doing.affair;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.motion.MotionLog;
import cn.flyingwings.robot.FaceService.FaceService;

/**
 * 事务模型，初始化时填充 {@link #name} 字段<br>
 * 所有的事务都集成此模型，实现其中的 {@link RobotAffair#onCreated(RobotAction)} 和 {@link #onFinished()} 接口。
 * @author gd.zhang
 *
 */
public abstract class RobotAffair extends HandlerThread{
	
	private static final int MSG_START 	= 0x01;
	private static final int MSG_STOP 	= 0x02;
	private static final int MSG_TODO	= 0x03;
	
	/**
	 * 事务名称，返回事务的真实名称。
	 */
	public abstract String name();
	private byte[] bytelock = new byte[0];
	private boolean onLooperHasrun = false;
	private Handler handler;
	protected RobotAffairManager mManager;
	private volatile boolean isFinish = false;
	private Lock lockaffair =  new ReentrantLock();  
	
	/**
	 * 检测事务，被动超时退出
	 */
	private Lock lock = new ReentrantLock(); // for Affair Dect quit。
	private int  times = 60*2;
	private ScheduledThreadPoolExecutor stpe = null;
	public  static  Map<String,Boolean> autoQuitAffair  =  new HashMap<String, Boolean>();
	public  static  String  actionFilter = "cn.flyingwings.robot.doing.affair";
	static {
		// 主动退出事务
		autoQuitAffair.put(MoodRecurAffair.NAME, true);
		autoQuitAffair.put(MusicAffair.NAME, true);
		autoQuitAffair.put(SeekHome.NAME, true);
		autoQuitAffair.put(SetWiFi.NAME, true);
		autoQuitAffair.put(WizardAffair.NAME, true);
		autoQuitAffair.put(IdleAffair.NAME, true);
		autoQuitAffair.put(PathRecurAffair.NAME, true);
		autoQuitAffair.put(DanceAffair.NAME, true);
		autoQuitAffair.put(XmAffair.NAME, true);
		// 非主动退出
		autoQuitAffair.put(Call.NAME, false);
		autoQuitAffair.put(PathLearnAffair.NAME, false);
		autoQuitAffair.put(AlarmAffair.NAME, false);
		autoQuitAffair.put(Charge.NAME, false);
		autoQuitAffair.put(ChatAffair.NAME, false);
		autoQuitAffair.put(ClientControl.NAME, false);
		autoQuitAffair.put(PowerOffAffair.NAME, false);
		autoQuitAffair.put(RobotInfoAffair.NAME, false);
		autoQuitAffair.put(SmartDeviceAffair.NAME, false);
		autoQuitAffair.put(SoundPlayAffair.NAME, false);
		autoQuitAffair.put(UpdateAffair.NAME, false);
		autoQuitAffair.put(MoodInfoAffair.NAME, false);
	}
	
	
	public RobotAffair(RobotAffairManager am) {
		super("RobotAffair");
		mManager = am;
		Robot.getInstance().getContext().registerReceiver(receiver, new IntentFilter(actionFilter));
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			stopAffair(true);
		}
		
	};
	
	@Override
	protected final void onLooperPrepared() {
		handler = new MyHandler(getLooper());
		super.onLooperPrepared();
		synchronized (bytelock) {
			bytelock.notify();
			onLooperHasrun = true;
		}
	}
	
	public void waitAffairReady() {
		synchronized (bytelock) {
			try {
				if(onLooperHasrun)
					return;
				bytelock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 通知事务开始运行
	 * @param act 引起事务启动的任务
	 */
	public final void startAffair(RobotAction act) {
		if(getLooper() ==null ) {
			RobotDebug.d("RobotAffair", "start affair , looper is null. action name: " + act.name());
			return;
		}
		if(act == null && !this.name().equals(IdleAffair.NAME)){
			RobotDebug.d("RobotAffair", "start affair " + this.name() + " act is null");
			return;
		}
		final Message msg = handler.obtainMessage(MSG_START, act);
		handler.sendMessage(msg);
	}
	
	/**
	 * 事务开始运行的初始化
	 * @param act 引起事务启动的任务
	 */
	protected final void create(RobotAction act) {
		RobotDebug.d("RobotAffair", "Affair : " +  this.name() + " onCreated 1。");
		onCreated(act);
		RobotDebug.d("RobotAffair", "Affair : " +  this.name() + " onCreated 2。"); 
		if(act == null) {
			RobotDebug.d("RobotAffair", "Affair : " +  this.name() + " onCreated 3。 act is null");
			return;
		}
		final Message msg = handler.obtainMessage(MSG_TODO, act);
		handler.sendMessage(msg);
	}
	
	/**
	 * 事务开始运行的回调函数
	 * 重载该函数来执行事务启动需要的功能。
	 * @param act 引起事务启动的任务
	 */
	protected void onCreated(RobotAction act) {
	}
	
	/**
	 * 机器人系统批准了执行一个任务的通知函数
	 * 通知当前事务系统接受了一个任务请求，任务请求并不需要事务做实际的执行。
	 * @param act 任务对象
	 */
	protected void onAction(RobotAction act) {
		
	}
	
	
	
	/**
	 * 任务执行完成通知函数
	 */
	protected void onActionDoingFinish() {
		if(!autoQuitAffair.containsKey(this.name()))
			return;
		if(autoQuitAffair.get(this.name()))
			return;
		lock.lock();
		times = 60 *2;
		lock.unlock();
		if(stpe == null) {
			stpe = new ScheduledThreadPoolExecutor(1);
			stpe.scheduleAtFixedRate(task,0, 1, TimeUnit.SECONDS); // 间隔1秒执行一次
			RobotDebug.d("RobotAffair", "onActionDoingFinish");
		}
	}
	
	
	
	/**
	 * 停止事务运行  
	 * 该方法会被事务管理器调用，也会被事务本身调用，所以要保持互斥。
	 * @param isself 
	 *        true: 事务本身自己结束自己 
	 *        false: 事务被事务管理器结束
	 */
	public final void stopAffair(boolean isself) {
		lockaffair.lock();
		if(stpe != null) {
			RobotDebug.d("RobotAffair", "停止检测...");
			stpe.shutdownNow();
			stpe.purge();
			stpe = null;
		}
		if(isFinish) {
			RobotDebug.e("RobotAffair", "Affair : " +  this.name() + " has isFinish.");
			lockaffair.unlock();
			return;
		}
		if(this.getLooper() == null) {
			RobotDebug.e("RobotAffair", "Looper is NULL Affair : " +  this.name() + " has stopped.");
			lockaffair.unlock();
			return;
		} else {
			RobotDebug.d("RobotAffair", "Stopping Affair : " +  this.name());
			onFinished();
			handler.removeMessages(MSG_START);
			handler.removeMessages(MSG_STOP);
			handler.removeMessages(MSG_TODO);
			if(this.getLooper() != null && (!isFinish)) {
				isFinish = true;
				this.getLooper().quit();
			}
			Robot.getInstance().getContext().unregisterReceiver(receiver);
			/**必须在此进行释放，因为事务管理器中的finish方法和switchAffair互斥的 
			 * 如果此处不释放，放置到该函数结尾进行释放，或者使用  synchronized进行互斥，
			 * 会导致死锁问题，请注意*/
			lockaffair.unlock(); 
			if(isself) {
				/*事务结束自己，需要通知事务管理器*/
				mManager.finished(this);
			}
		}
	}
	
	/**
	 * 停止当前事务的运行。
	 */
	protected final synchronized void finish() {
//		onFinished();
//		if(getLooper() != null) {
//			handler.removeMessages(MSG_START);
//			handler.removeMessages(MSG_STOP);
//			handler.removeMessages(MSG_TODO);
//			getLooper().quit();
//		}
//		mManager.finished(this);
	}
	
	/**
	 * 事务退出的回调函数，重载该函数完成事务退出的清理。<br>
	 * <b>此时消息循环已经退出，请直接完成功能，不要使用消息循环。</b>
	 */
	protected void onFinished() {
	}
	
	/**
	 * 处理一个兼容本事务的任务
	 * @param act 请求的任务
	 */
	public final void toDo(RobotAction act) {
		if(this.getLooper() == null) {
			RobotDebug.e("RobotAffair", "int RobotAffair toDo  looper is null action name : " + act.name());
			return ;
		}
		if(this.getLooper() != null && (!isFinish)) {
			final Message msg = handler.obtainMessage(MSG_TODO, act);
			handler.sendMessage(msg);
		} else {
			RobotDebug.e("RobotAffair", this.name() + " toDo " + act.name() + " has finished.");
		}
	}
	
	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			lockaffair.lock();
			switch (msg.what) {
			case MSG_START:
				RobotDebug.d("RobotAffair", "MSG_START 1.");
				create((RobotAction)msg.obj);
				RobotDebug.d("RobotAffair", "MSG_START 2.");
				break;
			case MSG_STOP:
				RobotDebug.d("RobotAffair", "MSG_STOP 1.");
				finish();
				RobotDebug.d("RobotAffair", "MSG_STOP 2.");
				break;
			case MSG_TODO:
				RobotDebug.d("RobotAffair", "onAction 0.");
				final RobotAction act = (RobotAction)msg.obj;
				RobotDebug.d("RobotAffair", "onAction 1. " + act.name() + "  type: " + act.type());
				if(act != null && act.type() != RobotAction.ACTION_TYPE_VIRTUAL) {
					RobotDebug.d("RobotAffair", "onAction 2." + act.name());
					onAction(act);
					RobotDebug.d("RobotAffair", "onAction 3." + act.name());
					act.doing();
					RobotDebug.d("RobotAffair", "doing    4." + act.name());
				}
				onActionDoingFinish();
				break;
			default:
				RobotDebug.out("Unknow message "+msg.what);
				break;
			}
			lockaffair.unlock();
			super.handleMessage(msg);
		}
	}	
	
	
	 private TimerTask task = new TimerTask() {
		 
		 @Override
		 public void run() {
			     lock.lock();
			     times--;
			     // 统计超过2分钟，结束事务
			     if(times <= 0) {
			    	 times = 120;
			    	 lock.unlock();
			    	 RobotDebug.d("RobotAffair", "超过2分钟需要退出");
			    	 // 避免主线程结束，子线程还在执行，将stopaffair交由系统broadcastreceiver执行
			    	 Robot.getInstance().getContext().sendBroadcast(new Intent(actionFilter));
    		    	return;
			     }
			     RobotDebug.d("RobotAffair", "检测是否超过2分钟");
			     lock.unlock();
		}
	 };
	
	
	
}
