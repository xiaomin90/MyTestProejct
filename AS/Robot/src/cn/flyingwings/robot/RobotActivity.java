package cn.flyingwings.robot;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;
import android.view.KeyEvent;
import android.widget.Toast;
import cn.flyingwings.robot.FaceService.BitmapCache;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.doing.event.ClientChange;
import cn.flyingwings.robot.service.RobotLightEffectsService;
import cn.flyingwings.robot.service.RobotLightEffectsService.LIGHT_EFFECTS;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import cn.flyingwings.robot.smartdeviceservice.Dev433Service;
import cn.flyingwings.robot.xmppservice.XmppLog;
import cn.flyingwings.robot.xmppservice.XmppService;

public class RobotActivity extends Activity {
	private String TAG = "RobotActivity";
	private long exitTime = 0;
	public static final boolean DEVELOPER_MODE = false;
	public FWZhuMuBoardcastreceiver receiver = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEVELOPER_MODE) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build());
		}

		setContentView(R.layout.activity_robot);

		/**
		 * 初始化机器人所有服务
		 */
		Robot.getInstance().start(this);
		receiver = new FWZhuMuBoardcastreceiver();
		this.registerReceiver(receiver, new IntentFilter("com.flyingwings.zhumu.disconnect"));

		/**
		 * 等待服务完成
		 */
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			RobotDebug.d(TAG, "thread sleep e : " + e1.toString());
		}

		/**
		 * 开机成功，语音提示
		 */
		try {
			Robot.getInstance().getManager().toDo(new JSONObject().put("name", "sound_play").put("res", "boot_ok")
					.put("subName", "boot_ok").toString());
		} catch (JSONException e1) {
			RobotDebug.d(TAG, "sound_play boot_ok e : " + e1.toString());
		}

		/**
		 * 获取网络状态情况 放在网络状态BoardcastReciver下去维护
		 */
		RobotStatusInfoService info_service = (RobotStatusInfoService) Robot.getInstance()
				.getService(RobotStatusInfoService.NAME);
		if (info_service == null)
			return;
		if ((!info_service.getWifiEnableStatus()) || (!info_service.getWifiConStatus())
				|| (info_service.getWifiSSID() == null)) {
			/**
			 * 没有网络的情况下，需要语音提示配置网络
			 */
			try {
				JSONObject todo_json = new JSONObject();
				todo_json.put("name", "sound_play");
				todo_json.put("res", "internet_error");
				Robot.getInstance().getManager().toDo(todo_json.toString());
			} catch (JSONException e) {
				RobotDebug.d(TAG, "sound_play internet_error e : " + e.toString());
			}
			RobotLightEffectsService lighteffect = (RobotLightEffectsService) Robot.getInstance()
					.getService(RobotLightEffectsService.NAME);
			if (lighteffect != null) {
				if (lighteffect.setLightEffect(LIGHT_EFFECTS.Net_Connection_Fail))
					XmppLog.d(TAG, "网络连接失败设置灯效成功");
				else
					XmppLog.d(TAG, "网络连接失败设置灯效失败");
			}
		}

		/**
		 * 设置正常工作时的灯效
		 */
		RobotLightEffectsService lighteffect = (RobotLightEffectsService) Robot.getInstance()
				.getService(RobotLightEffectsService.NAME);
		if (lighteffect != null) {
			if (lighteffect.setLightEffect(LIGHT_EFFECTS.Normal_Work_Status))
				XmppLog.d(TAG, "正常工作模式设置灯效成功");
			else
				XmppLog.d(TAG, "正常工作模式设置灯效失败");
		}

		/**
		 * 设置安防状态灯效
		 */
		RobotLightEffectsService lighteffectsService = (RobotLightEffectsService) Robot.getInstance()
				.getService(RobotLightEffectsService.NAME);
		if (lighteffectsService != null) {
			RobotDebug.d(TAG, "设置安防状态灯效");
			if (Dev433Service.ONSercurity)
				lighteffectsService.setLightEffect(LIGHT_EFFECTS.Normal_Work_Status);
			else
				lighteffectsService.setLightEffect(LIGHT_EFFECTS.Sercurity_Off);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		RobotDebug.d(TAG, "onResume....");
	}

	@Override
	protected void onPause() {
		super.onPause();
		RobotDebug.d(TAG, "onPause....");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void exit() {
		if ((System.currentTimeMillis() - exitTime) > 2000) {
			Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
			exitTime = System.currentTimeMillis();
		} else {
			finish();
			System.exit(0);
		}
	}
	
	@Override
	protected void onDestroy() {
		Robot.getInstance().stop();
		super.onDestroy();
		BitmapCache.getInstance().clearCache();
		ModuleController.getInstance().quit();
	}

	public class FWZhuMuBoardcastreceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			RobotDebug.d(TAG, " FWZhuMuBoardcastreceiver onReceive  出现弹框 ");
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			try {
				xmpp.sendMsg("R2C", new JSONObject().put("opcode", "92903").put("reason", "通话过程中出现弹框").toString(),
						92903);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			RobotDebug.d(TAG, "FWZhuMuBoardcastreceiver onReceive 出现弹框 ");
		}
	}

}
