package cn.flyingwings.robot.motion;

import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class MoveDect extends BroadcastReceiver {
	
	private  String TAG = "MoveDect";
	
	enum  Action_Type{
		ACTION,STOP
	};
	
	private  String ACTION_BOARDCAST = "cn.flyingwings.johnson.MoveDect";
	private  ScheduledThreadPoolExecutor stpe = null; 
	private  Action_Type  type = Action_Type.ACTION;
	private  TimerTask    task=  null;
	private  String json_format = "{\"opt\":%d,\"act\":%d,\"left_speed\":%d,\"right_speed\":%d,\"header_h\":%d,\"header_v\":%d,\"ang\":%d,\"distance\":%d}"; 
	private  Lock    lock;
	private  int     Times = 20;
	
	private MoveDect()
	{
		 IntentFilter localIntentFilter = new IntentFilter();
		 localIntentFilter.addAction(ACTION_BOARDCAST);
		 Robot.getInstance().getContext().registerReceiver(this, localIntentFilter);
		 lock = new ReentrantLock();
		 task = new TimerTask()
		 {
			 @SuppressWarnings("unused")
			 public void run() {
				     lock.lock();
				     Times--;
					 if( (Times <= 0 ) && (type == Action_Type.ACTION))
					 {
						 	String str= String.format(json_format,0,1,0,0,0,0,0,0);
							JSONObject json = null;
							try {
								json = new JSONObject(str.toString());
								json.put("name", "client_move");
								json.put("subName", "motion_stop");
							} catch (JSONException e) {
								e.printStackTrace();
								MotionLog.e(TAG, "in task run string not json fomart.");
								return;
							}
							if(json == null)
								return;
							Robot.getInstance().getManager().toDo(json.toString());
							// send Broadcast;
							MotionLog.d(TAG," Times <=0  type = ACTION .");
							Intent intent = new Intent(ACTION_BOARDCAST);
							if(Robot.getInstance().getContext() != null)
								Robot.getInstance().getContext().sendBroadcast(intent);
					 } 
					 else if( (Times <= 0) && (type == Action_Type.STOP))
					 {
						 // send Broadcast;
						 MotionLog.d(TAG," Times <=0  type = STOP .");
						 Intent intent = new Intent(ACTION_BOARDCAST);
						 if(Robot.getInstance().getContext() != null)
							 Robot.getInstance().getContext().sendBroadcast(intent);
					 }
					 lock.unlock();
			}
		 };
	}
		
	private volatile static MoveDect   instance = null;
	
	/**
	 * jdk 1.5  双重检查锁定才能够正常达到单例效果
	 * @return MoveDect instance
	 */
	public static  MoveDect getInstance() 
	{  
		if (instance == null) {  
			 synchronized (MoveDect.class) {  
		        if (instance == null) {  
		        	instance = new MoveDect();  
		        }   
			 }
		}  
	    return instance;  
	}  	
	
	public  void setHasActMsg()
	{
		lock.lock();
		this.Times = 20;
		this.type = Action_Type.ACTION;
		lock.unlock();
		if(stpe == null)
		{
			stpe = new ScheduledThreadPoolExecutor(1);
			stpe.scheduleAtFixedRate(task, 0, 100, TimeUnit.MILLISECONDS);// 延迟100ms 执行
		}
	}
	
	public  void cancel()
	{
		lock.lock();
		this.type = Action_Type.STOP;
		lock.unlock();
	}

	public boolean isMotionStop() {
		if((Times <= 0) || (this.type == Action_Type.STOP) ) {
			return true;
		}
		return false;
	} 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//MotionLog.d(TAG,"onReceive cancel thread.");
		if((stpe != null)){
			stpe.shutdownNow();
			stpe.purge();
			stpe = null;
		}
	}
	
	public void destory()
	{
		 Robot.getInstance().getContext().unregisterReceiver(this);
		 instance = null;
	}
	
}
