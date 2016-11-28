package cn.flyingwings.robot.zhumu;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import us.zoom.sdk.MeetingActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;
import cn.flyingwings.robot.R;
import cn.flyingwings.robot.RemoteService;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.FaceService.FaceData;
import cn.flyingwings.robot.FaceService.FaceData.Face;
import cn.flyingwings.robot.FaceService.XmlDomService;

import com.android.zhumu.ZhuMuStatus;

/**
 * 瞩目会议Activity
 * @author js.min
 *
 */
public class RobotMeetingActivity extends MeetingActivity {
	private static String TAG = "RobotMeetingActivity";
	private ImageView imageview = null;
	private FaceData  mFaceData = null;
	private AssetManager mAssetManager;
	private BitmapFactory.Options options = new BitmapFactory.Options();
	private Matrix matrix = new Matrix();
	private MyHandler handler = null;
	private Timer timer = null;
	private TimerTask task = null;
	private int index = 0;
	private boolean isServiceRegister = false;
	private Messenger mRemoteMessenger = null;
	private Messenger mClientMessenger = null;
	private boolean hasDailog = true;
	/**
	 * 进程通信获取当前会议类型设置UI
	 */
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			RobotDebug.d(TAG, "onServiceConnected");
			mRemoteMessenger = new Messenger(service);
            //注意obtain第一个参数，前面文章有解释
            Message message = Message.obtain(null, 1);
            message.replyTo = mClientMessenger;
            try {
                mRemoteMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			RobotDebug.d(TAG, "onServiceDisconnected");
		}
		
	};
	
	@Override
	protected int getLayout() {
		return R.layout.robot_meeting;
	}
	
	public  BroadcastReceiver  leaveReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				leaveCall();
			}
	};
	public static final String action = "cn.flyingwings.robot.zhumu.leavemeeting";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFaceData = XmlDomService.parseData("resXml/remote_audio.xml", this);
		mAssetManager = this.getAssets();
		options.inPreferredConfig = Bitmap.Config.RGB_565;  
		options.inSampleSize = 2;
		matrix.preScale(2, 2, 0f, 0f);
		handler = new MyHandler(this.getMainLooper());
		timer   = new Timer();
	    task = new TimerTask() {
	        public void run() {
	        	index++;
	        	if(index > mFaceData.loop.size())
	        		index = 1;
	            Message message = new Message();
	            message.what = index;
	            handler.sendMessage(message);
	        }
	    };
		imageview = (ImageView) findViewById(R.id.audiofaceview);
		RobotDebug.d(TAG, "Thread1 : " + Thread.currentThread().getId());
		this.registerReceiver(leaveReceiver,new IntentFilter(action));
		AudioManager mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		int volume = 3;
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		RobotDebug.d(TAG, "onStart ....");
	    mClientMessenger = new Messenger(handler);
	    bindService(new Intent(this, RemoteService.class), connection, Context.BIND_AUTO_CREATE);
	    isServiceRegister = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		overridePendingTransition(0, 0);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		RobotDebug.d(TAG, "onPause ....");
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		if(isServiceRegister == true){
			isServiceRegister = false;
			unbindService(connection);
		}
		hasDailog = false;
		this.unregisterReceiver(leaveReceiver);
	}

	@Override
	protected boolean isAlwaysFullScreen() {
		return true;
	}
	
	@Override
	protected boolean isSensorOrientationEnabled() {
		return false;
	}
	
	@Override
	protected void onMeetingConnected() {
		RobotDebug.d(TAG, "onMeetingConnected 1... : ");
		rotateVideoDevice(ZhuMuStatus.VideoRotationAction.VIDEO_ROTATION_ACTION_CLOCK180);
	}
	
	@Override
	protected void onSilentModeChanged(boolean inSilentMode) {
		RobotDebug.d(TAG, "onSilentModeChanged  : " + inSilentMode);
	}
	
	@Override
	protected void onStartShare() {
		
	}

	@Override
	protected void onStopShare() {
		
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		RobotDebug.d(TAG, "onWindowFocusChanged 1... : " + hasFocus);
		if(!hasFocus && hasDailog) {
			this.getWindowManager().removeView(this.getWindow().getDecorView());
			RobotDebug.d(TAG, "onWindowFocusChanged disconnect 2... : " + hasFocus);
			this.sendBroadcast(new Intent("com.flyingwings.zhumu.disconnect"));
			leaveCall();
		}
	}
	
	/**
	 * 绘制图像handler
	 * @author js.min
	 *
	 */
	private class MyHandler extends Handler {
		
		public MyHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(msg.what < 99){
				drawFaces(mFaceData.getLoop().get(msg.what-1));
			} else if(msg.what == 100) {
				//audio_video
				RobotDebug.d(TAG, "MyHandler audio_video");
				imageview.setVisibility(View.INVISIBLE);
			} else if(msg.what == 101) {
				//audio
				RobotDebug.d(TAG, "MyHandler audio");
				imageview.setVisibility(View.VISIBLE);
				if(timer != null && task != null)
					timer.schedule(task, 2*1000, 200);
			} else if(msg.what == 102) {
				RobotDebug.d(TAG, "MyHandler unknown");
				imageview.setVisibility(View.INVISIBLE);
			} else if(msg.what == 99) {
				RobotDebug.d(TAG, "MyHandler View");
				imageview.setVisibility(View.INVISIBLE);
				muteAudio(true);
			}  
		}
	}
	
	/**
	 * 从asset 路径读取bitmap 图像
	 * @param file
	 * @return
	 */
	private Bitmap creatBitmap(String file) {
		Bitmap bmp = null;
		try {
			bmp = BitmapFactory.decodeStream(mAssetManager.open(file), null, options);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return bmp;
	}
	
	/**
	 * 设置图片
	 * @param face
	 */
	private void drawFaces(Face face) { 
		Bitmap bmp = creatBitmap(face.fileName);
		if(bmp == null)
			return;
		imageview.setImageBitmap(bmp);
	}
}
