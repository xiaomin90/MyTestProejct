package cn.flyingwings.robot.doing.event;

import java.util.HashMap;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotDivide;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * 事件响应器<br>
 * 将事件按照名称进行响应，调用对应的响应项解析事件，提交动作请求。
 * @author gd.zhang
 *
 */
public class RobotEventMap extends HandlerThread{
	private final static String TAG = "RobotEventMap";
	
	private static final int MSG_EVENT = 0x01;
	
	private HashMap<String, RobotEvent> eventMap = new HashMap<String, RobotEvent>();
	
	private Handler handler;
	private RobotDivide divider;
	
	public RobotEventMap(RobotDivide d) {
		super("RobotEventMap");
		divider = d;
		AllEvent.initAllEvent(this);
	}
	
	@Override
	protected void onLooperPrepared() {
		handler = new MyHandler(this.getLooper());
		super.onLooperPrepared();
	}
	
	/**
	 * 添加一个事件映射项
	 * @param e 事件映射项
	 * @return 是否添加成功，如果事件映射已经存在则添加失败
	 */
	public boolean addEvent(RobotEvent e) {
		if (e == null)
			return false;
		if (!(e instanceof RobotEvent))
			return false;
		if (eventMap.containsKey(e.name()))
			return false;
		eventMap.put(e.name(), e);
		return true;
	}
	
	/**
	 * 将一个事件放入事件队列中，稍候进行映射
	 * @param intent 事件内容
	 * @return 是否加入队列成功
	 */
	public boolean mapEvent(Intent intent) {
		if( (handler == null) || (getLooper() == null))
			return false;
		final Message msg = handler.obtainMessage(MSG_EVENT, intent);
		return handler.sendMessage(msg);
	}
	
	/**
	 * 停止事件映射器的运行
	 */
	public void stopDoing() {
		getLooper().quit();
	}
	
	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		public MyHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_EVENT:
				Intent intent = (Intent)msg.obj;
				String name = intent.getStringExtra("name");
				if (name == null) {
					RobotDebug.d(TAG, "event without name ");
					break;
				}
				RobotEvent e = eventMap.get(name);
				if (e == null) {
					RobotDebug.d(TAG, "Unknow event "+name);
					break;
				}
				e.map(divider, intent);
				break;
			default:
				RobotDebug.d(TAG, "Unknow message "+msg.what);
				break;
			}
			super.handleMessage(msg);
		}
	}
}
