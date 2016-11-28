package cn.flyingwings.robot;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import cn.flyingwings.robot.zhumu.ZhuMuService;
import cn.flyingwings.robot.zhumu.ZhuMuService.ZHUMUMeetingType;

public class RemoteService extends Service {

	private String TAG = "RemoteService";
	private RemoteHandler remotehandler = null;
	private Messenger mRemoteMessenger = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		RobotDebug.d(TAG, "onCreate");
		remotehandler  = new RemoteHandler();
		mRemoteMessenger = new Messenger(remotehandler);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return (mRemoteMessenger != null)?mRemoteMessenger.getBinder():null;
	}
	
	 @SuppressLint("HandlerLeak")
	 public class RemoteHandler extends Handler {
	        
		 	@Override
	        public void handleMessage(Message msg) {
	               //为了把消息回传给client端，所以获取client端设置的Messenger
	               Messenger clientMessenger = msg.replyTo;
	               if (clientMessenger != null) {
	                        try {
	                            //注意obtain第一个参数，，因为参数target不可序列化
	                        	if(ZhuMuService.meetingtype == ZHUMUMeetingType.AUDIO) {
	                        		// msg.what  1 audio
	                        		RobotDebug.d(TAG, "RemoteHandler AUDIO");
	                        		clientMessenger.send(Message.obtain(null, 101, 0, 0));
	                        	} else if(ZhuMuService.meetingtype == ZHUMUMeetingType.VIDEO_AUIDO) {
	                        		// msg.what 2 audi_video
	                        		RobotDebug.d(TAG, "RemoteHandler VIDEO_AUIDO");
	                        		clientMessenger.send(Message.obtain(null, 100, 0, 0));
	                        	} else if(ZhuMuService.meetingtype == ZHUMUMeetingType.VIEW) {
	                        		// msg.what 0 view
	                        		clientMessenger.send(Message.obtain(null, 99, 0, 0));
	                        	} else {
	                        		// msg.waht 3 Unknown
	                        		RobotDebug.d(TAG, "RemoteHandler Unkown");
	                        		clientMessenger.send(Message.obtain(null, 102, 0, 0));
	                        	}
	                        } catch (RemoteException e) {
	                            e.printStackTrace();
	                        }
	               }
	       }
		   
		  /**
		   	 * 获取meeting type 类型 
		 	 * @return
		 	 */
		   public ZHUMUMeetingType getMeetingType(){
			   if(ZhuMuService.meetingtype == ZHUMUMeetingType.AUDIO) {
				   return ZHUMUMeetingType.AUDIO;
			   }else if(ZhuMuService.meetingtype == ZHUMUMeetingType.VIDEO_AUIDO) {
				   return ZHUMUMeetingType.VIDEO_AUIDO;
			   }else if(ZhuMuService.meetingtype == ZHUMUMeetingType.VIEW) {
				   return ZHUMUMeetingType.VIEW;
			   }
			   else {
				   return null;
			   }
		   }
	}

}
