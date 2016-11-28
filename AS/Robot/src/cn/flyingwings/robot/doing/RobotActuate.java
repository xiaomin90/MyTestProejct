package cn.flyingwings.robot.doing;

import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotAction;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * 动作执行器
 * 开启一个线程完成动作请求的功能。
 * 机器人系统中应该只有一个实例。
 * @author gd.zhang
 *
 */
public class RobotActuate extends HandlerThread{
	private final static String TAG = "RobotActuate";
	
	private final static int MSG_ACTUATE = 0x01;
	
	private Handler handler;
	
	public RobotActuate() {
		super("RobotActuate");
	}
	
	@Override
	protected void onLooperPrepared() {
		handler = new MyHandler(this.getLooper());
		super.onLooperPrepared();
	}
	
	/**
	 * 要求动作执行器处理一个动作请求
	 * @param act 动作请求
	 */
	public void actuate(RobotAction act) {
		final Message msg = handler.obtainMessage(MSG_ACTUATE);
		msg.obj = act;
		handler.sendMessage(msg);
		RobotDebug.d(TAG, "send msg "+msg.what);
	}
	
	/**
	 * 停止当前动作执行器的运行，忽略剩余的任务。
	 */
	public void stopDoing() {
		getLooper().quit();
	}
	
	private static class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_ACTUATE:
				final RobotAction act = (RobotAction)msg.obj;
				RobotDebug.d(TAG, "act doing ");
				act.doing();
				break;
			default:
				RobotDebug.d(TAG, "Not support message "+msg.what);
				break;
			}
			super.handleMessage(msg);
		}
	}

}
