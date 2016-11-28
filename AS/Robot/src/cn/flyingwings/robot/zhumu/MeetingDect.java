package cn.flyingwings.robot.zhumu;

import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.android.zhumu.ZhuMuStatus.MeetingStatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.motion.MotionLog;

public class MeetingDect extends BroadcastReceiver {
	
	private  String TAG = "MeetingDect";
	private  String ACTION_BOARDCAST = "cn.flyingwings.johnson.MeetingDect";
	private  ScheduledThreadPoolExecutor stpe = null; 
	private  TimerTask    task=  null;
	private  Lock    lock;
	private  int     Times = 40;
	public enum  Meeting_Status{
		start,inprocess
	}
	private Meeting_Status status = Meeting_Status.start;
	private MeetingDect() {
		 IntentFilter localIntentFilter = new IntentFilter();
		 localIntentFilter.addAction(ACTION_BOARDCAST);
		 Robot.getInstance().getContext().registerReceiver(this, localIntentFilter);
		 lock = new ReentrantLock();
		 task = new TimerTask() {
			 public void run() {
				     lock.lock();
				     Times--;
					 if((Times <= 0) && (status == Meeting_Status.start)) {
							// send Broadcast;
							MotionLog.d(TAG," 会议超时，结束会议");
							ZhuMuService zhumuservice = (ZhuMuService) Robot.getInstance().getService(ZhuMuService.NAME);
							zhumuservice.leaveRoom();
							Intent intent = new Intent(ACTION_BOARDCAST);
							if(Robot.getInstance().getContext() != null)
								Robot.getInstance().getContext().sendBroadcast(intent);
					 }
					 lock.unlock();
			}
		 };
	}
		
	private volatile static MeetingDect instance = null;
	
	/**
	 * jdk 1.5  双重检查锁定才能够正常达到单例效果
	 * @return MoveDect instance
	 */
	public static  MeetingDect getInstance() {  
		if (instance == null) {  
			 synchronized (MeetingDect.class) {  
		        if (instance == null) {  
		        	instance = new MeetingDect();  
		        }   
			 }
		}  
	    return instance;  
	}  	
	
	public  void setStartmeeting() {
		lock.lock();
		this.Times = 40;
		status = Meeting_Status.start;
		lock.unlock();
		if(stpe == null) {
			stpe = new ScheduledThreadPoolExecutor(1);
			stpe.scheduleAtFixedRate(task, 0, 1000, TimeUnit.MILLISECONDS);// 延迟1000ms 执行
		}
	}
	
	public  void setMeetingSuccess() {
		lock.lock();
		status = Meeting_Status.inprocess;
		lock.unlock();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if((stpe != null)) {
			stpe.shutdownNow();
			stpe.purge();
			stpe = null;
		}
	}
	
	public void destory() {
		lock.lock();
		if((stpe != null)) {
			stpe.shutdownNow();
			stpe.purge();
			stpe = null;
		}
		Robot.getInstance().getContext().unregisterReceiver(this);
		instance = null;
		lock.unlock();
	}

}
