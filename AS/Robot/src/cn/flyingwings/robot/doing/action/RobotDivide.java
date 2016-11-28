package cn.flyingwings.robot.doing.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;

import org.json.JSONObject;

import cn.flyingwings.robot.R;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotServerURL;
import cn.flyingwings.robot.doing.RobotActuate;
import cn.flyingwings.robot.doing.affair.RobotAffairManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * 动作解析器<br>
 * 接受 {@code JSON} 格式的动作请求，转换成动作对象。按照动作对象的类型，简单动作提交到执行器，任务在控制表中查找和当前事务的关系并处理。
 * 
 * @author gd.zhang
 *
 */
public class RobotDivide extends HandlerThread {
	private static final String TAG = "RobotDivide";

	private static final int MSG_TO_DO = 0x01;

	public static final int COMPITIBILITY_ALLOWED = 0x01;
	public static final int COMPITIBILITY_INCOMPAT = 0x02;
	public static final int COMPITIBILITY_BLOCKED = 0x03;

	private HashMap<String, Class<RobotAction>> actionSet = new HashMap<String, Class<RobotAction>>();

	private HashMap<String, HashMap<String, Integer>> controlList = new HashMap<String, HashMap<String, Integer>>();

	private HashMap<String, String> action2affair = new HashMap<String, String>();

	private Handler handler;

	private RobotActuate actuator;
	private RobotAffairManager affairManager;

	public RobotDivide(RobotActuate a, RobotAffairManager am) {
		super(TAG);
		actuator = a;
		affairManager = am;
		AllAction.initAllAction(this);
		/**
		 * TODO 修改控制表的子啊如路径，考虑从载入默认控制表文件和从网络上更新控制表文件。
		 */
		Log.d(TAG, "Start divider");
//		Context mContext = Robot.getInstance().getContext();
//		File clf = mContext.getFileStreamPath(RobotServerURL.ActionAffairPath);
//		if (!clf.exists()) {
//			copyFile(mContext, R.raw.robot2_0_0_26, clf);
//		}
//		loadControlFile(clf.getAbsolutePath());
		loadfileFormAssets();
	}

	@Override
	protected final void onLooperPrepared() {
		handler = new MHandler();
		super.onLooperPrepared();
	}

	/**
	 * 向动作解析器中添加一个动作请求
	 * 
	 * @param a
	 *            动作请求的 JSON 字符串
	 * @return 是否添加成功
	 */
	public boolean toDo(String a) {
		final Message msg = handler.obtainMessage(MSG_TO_DO, a);
		return handler.sendMessage(msg);
	}

	/**
	 * 添加一个动作的定义
	 * 
	 * @param n
	 *            动作名称
	 * @param c
	 *            动作的实现
	 * @return 是否添加成功，如果已经存在，则添加失败
	 */
	public boolean addAction(String n, Class<?> c) {
		if (!RobotAction.class.isAssignableFrom(c))
			return false;
		if (actionSet.containsKey(n))
			return false;
		@SuppressWarnings("unchecked")
		Class<RobotAction> cc = (Class<RobotAction>) c;
		actionSet.put(n, cc);
		return true;
	}

	/**
	 * 加载控制表和任务的目标事务
	 * @return
	 */
	public boolean loadfileFormAssets(){
		BufferedReader br = null;
		Context  context = Robot.getInstance().getContext();
		AssetManager assMg = context.getAssets();
		InputStream inStrem = null;
		String clfpath = RobotServerURL.ActionAffairPath;
		try {
			inStrem = assMg.open(clfpath);
			br = new BufferedReader(new InputStreamReader(inStrem));
		} catch (IOException e1) {
			e1.printStackTrace();
			Log.d(TAG, "Load control file : " + e1);
			return false;
		}
		
		controlList.clear();
		action2affair.clear();

		String line;
		String[] affairs = null;
		String[] values = null;
		int i;
		try {
			// 加载控制列表文件中第一部分，兼容性表
			while (br.ready()) {
				line = br.readLine();
				if (line.startsWith("#") || line.length() < 2) {
					continue;
				}
				if (line.startsWith("action2affair"))
					break;
				if (affairs == null) {
					affairs = line.split("\\s+");
				} else {
					HashMap<String, Integer> hm = new HashMap<String, Integer>();
					values = line.split("\\s+");
					for (i = 1; i < values.length; i++) {
						int v = Integer.parseInt(values[i]);
						hm.put(affairs[i], v);
					}
					controlList.put(values[0], hm);
				}
			}
			// 加载控制列表文件中第二部分，任务到目标事务的关系
			while (br.ready()) {
				line = br.readLine();
				if (line.startsWith("#")) {
					continue;
				}
				values = line.split("\\s+");
				action2affair.put(values[0], values[1]);
			}
		} catch (Exception e) {
			RobotDebug.d(TAG, "br read failed : " + e.getLocalizedMessage());
		}

		try {
			if (br != null)
				br.close();
			if(inStrem != null)
				inStrem.close();
		} catch (Exception e) {
			RobotDebug.d(TAG, "br close failed : " + e.getLocalizedMessage());
		}
		Log.d(TAG, "Load control file finished, " + controlList.size() + ", " + action2affair.size());
		return true;

	}
	
	
	
	
	/**
	 * 加载控制表和任务的目标事务
	 * 
	 * @param f
	 *            文件路径
	 * @return 是否成功加载
	 */
	public boolean loadControlFile(String f) {
		File file = new File(f);
		BufferedReader br = null;
		if (!file.exists()) {
			RobotDebug.d(TAG, "Control file " + f + " not exists ");
			return false;
		}
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (Exception e) {
			RobotDebug.d(TAG, "Open control file failed " + e.getMessage());
			return false;
		}
		
		controlList.clear();
		action2affair.clear();

		String line;
		String[] affairs = null;
		String[] values = null;
		int i;
		try {
			// 加载控制列表文件中第一部分，兼容性表
			while (br.ready()) {
				line = br.readLine();
				if (line.startsWith("#") || line.length() < 2) {
					continue;
				}
				if (line.startsWith("action2affair"))
					break;
				if (affairs == null) {
					affairs = line.split("\\s+");
				} else {
					HashMap<String, Integer> hm = new HashMap<String, Integer>();
					values = line.split("\\s+");
					for (i = 1; i < values.length; i++) {
						int v = Integer.parseInt(values[i]);
						hm.put(affairs[i], v);
					}
					controlList.put(values[0], hm);
				}
			}
			// 加载控制列表文件中第二部分，任务到目标事务的关系
			while (br.ready()) {
				line = br.readLine();
				if (line.startsWith("#")) {
					continue;
				}
				values = line.split("\\s+");
				action2affair.put(values[0], values[1]);
			}
		} catch (Exception e) {
			RobotDebug.d(TAG, e.getLocalizedMessage());
		}

		try {
			if (br != null)
				br.close();
		} catch (Exception e) {
		}
		Log.d(TAG, "Load control file finished, " + controlList.size() + ", " + action2affair.size());
		return true;
	}

	/**
	 * 停止动作解析器
	 */
	public void stopDoing() {
		getLooper().quit();
	}

	private void copyFile(Context context, int id, File dst) {
		try {
			InputStream orig = context.getResources().openRawResource(id);
			OutputStream fresh = new FileOutputStream(dst);
			byte[] buff = new byte[4096];
			int ret = orig.read(buff);
			while (ret > 0) {
				fresh.write(buff, 0, ret);
				ret = orig.read(buff);
			}
			orig.close();
			fresh.flush();
			fresh.close();
		} catch (Exception e) {
			Log.d(TAG, "Copy file errer", e);
		}
	}
	
	

	@SuppressLint("HandlerLeak")
	private class MHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TO_DO:
				RobotAction action = null;
				final String act = (String) msg.obj;
				Log.d(TAG, act);
				try {
					final JSONObject json = new JSONObject(act);
					final String name = json.getString("name");
					final Class<RobotAction> ca = actionSet.get(name);
					action = ca.newInstance();
					/* 按照名称，查找到了动作对象，判断对象是否正确 */
					if ((action == null) || !name.equals(action.name())) {
						RobotDebug.d(TAG, "Action name not match request name !");
						break;
					}
					action.parse(act);
				} catch (Exception e) {
					RobotDebug.d(TAG, "Can not create this action " + act);
					Log.d(TAG, "Json error", e);
					break;
				}
				/* 正确找到动作，开始判断动作类型 */
				if (action.type() == RobotAction.ACTION_TYPE_SIMPLE) {
					RobotDebug.d(TAG, "simple task action :" + action.name());
					action.changeFace();
					actuator.actuate(action);
				} else if (action.type() == RobotAction.ACTION_TYPE_TASK
						|| action.type() == RobotAction.ACTION_TYPE_VIRTUAL) {
					/* 动作类型为任务，查找控制表，并进行处理 */
					if (!controlList.containsKey(action.name())) {
						RobotDebug.d(TAG, "Unknow task " + action.name());
						break;
					}
					if(affairManager.currentAffair() == null) {
						RobotDebug.e(TAG, "currentAffair is null,need to do it again.");
						Robot.getInstance().getManager().toDo(act);
						break;
					}
					Integer control = controlList.get(action.name()).get(affairManager.currentAffair());
					if (control == null) {
						RobotDebug.d(TAG, "Not find current affair " + affairManager.currentAffair());
						break;
					}

					RobotDebug.d(TAG, "current affair: " + affairManager.currentAffair());
					switch (control) {
					case COMPITIBILITY_ALLOWED:
						RobotDebug.d(TAG, "COMPITIBILITY_ALLOWED  Action   " + act);
						action.changeFace();
						affairManager.toDo(action);
						break;
					case COMPITIBILITY_INCOMPAT:
						RobotDebug.d(TAG, "COMPITIBILITY_INCOMPAT  Action   " + act);
						String affair = action2affair.get(action.name());
						affairManager.switchAffair(affair, action);
						action.changeFace();
						break;
					case COMPITIBILITY_BLOCKED:
						RobotDebug.d(TAG, "Action blocked " + act);
						break;
					default:
						RobotDebug.d(TAG, "Unknow message " + msg.what);
						break;
					}
				} else {
					RobotDebug.d(TAG, "Unknow action type " + action.type());
				}
				break;
			default:
				RobotDebug.d(TAG, "Unknow message " + msg.what);
				break;
			}
			super.handleMessage(msg);
		}
	}
}
