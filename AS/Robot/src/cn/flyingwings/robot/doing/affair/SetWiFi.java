package cn.flyingwings.robot.doing.affair;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.BarCodeService;

public class SetWiFi extends RobotAffair {

	public static final String NAME = "set_wifi";
	
	private class WifiConfig{
		String type;
		String ssid;
		String passwd;
		boolean hidden;	
	}
	private WifiConfig mWifiConf;
	
	/* if the target wifi is hidden, try wpa, wep one by one. */
	private final static int MSG_TRY_WPA = 0X01;
	private final static int MSG_TRY_WEP = 0X02;
	private final static int MSG_TIMEOUT = 0X10;
	private Handler mHandler;
	
	private BarCodeService barCode;
	private WifiManager mWifi;
	private static boolean isfinished = false;
	
	
	private void parseWifiInfo(ScanResult wifi, WifiConfig conf){
		if(conf.passwd.isEmpty()){
			conf.type = "nopass";
			return ;
		}
		String capabilities = wifi.capabilities;
		if(capabilities.contains("WPA") || capabilities.contains("wpa")){
			conf.type = "WPA";
		}else if(capabilities.contains("WEP") || capabilities.contains("wep")){
			conf.type = "WEP";
		}
	}
	
	@SuppressLint("DefaultLocale") 
	private void connectWifi(WifiConfig conf) {
		Log.d(NAME, "type: " + conf.type + ", ssid: " + conf.ssid + ", passwd: " + conf.passwd + ", hidden: " + conf.hidden);
		List<WifiConfiguration> confList = mWifi.getConfiguredNetworks();
		if (confList == null)
		{
			Log.d(NAME, "添加WiFi设置失败  " + confList);
			if(isfinished == false)
				stopAffair(true);
			return;
		}
		for (WifiConfiguration c:confList) {
			if (c.SSID.equals(conf.ssid)) {
				mWifi.removeNetwork(c.networkId);
			}
		}
		
		WifiConfiguration c = new WifiConfiguration();
		c.SSID = '"'+conf.ssid+'"';
		c.hiddenSSID = conf.hidden;
		c.allowedAuthAlgorithms.clear();
		c.allowedGroupCiphers.clear();
		c.allowedKeyManagement.clear();
		c.allowedPairwiseCiphers.clear();
		c.allowedProtocols.clear();
		c.status = WifiConfiguration.Status.ENABLED;
		if("nopass".equals(conf.type)) {
			c.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			c.wepTxKeyIndex = 0;
		} else if("WEP".equals(conf.type)) {
			c.wepKeys[0] = '"'+conf.passwd+'"';
			c.wepTxKeyIndex = 0;
			c.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			c.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		} else if ("WPA".equals(conf.type.toUpperCase())) {
			c.preSharedKey = '"'+conf.passwd+'"';
			c.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			c.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			c.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			c.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			c.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		} else {
			Log.d(NAME, "不支持的wifi加密类型"+conf.type);
			if(isfinished == false)
				stopAffair(true);
			return;
		}
		
		int id = mWifi.addNetwork(c);
		if(id < 0) {
			Log.d(NAME, "添加WiFi设置失败");
			if(conf.hidden){
				// wait the continuing operations...
			}else{
				// voice tips 
				if(isfinished == false)
					stopAffair(true);
			}
			return;
		}
		
		mWifi.saveConfiguration();
		mWifi.disconnect();
		mWifi.enableNetwork(id, true);
		mWifi.reconnect();
		
		Robot.getInstance().getContext().registerReceiver(connectReceiver, connectFilter);
		connecting = true;
	}
	
	private boolean scanning = false;
	private IntentFilter scanFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			scanning = false;
			Robot.getInstance().getContext().unregisterReceiver(this);
			
			List<ScanResult> scanR = mWifi.getScanResults();
			boolean found = false;
			for (ScanResult r:scanR) {
				if(r.SSID.equals(mWifiConf.ssid)) {
					found = true;
					parseWifiInfo(r, mWifiConf);
					connectWifi(mWifiConf);
					mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, 1000 * 15);
					break;
				}
			}
			
			if(found){
				Log.d(NAME, "WIFI found: " + mWifiConf.ssid);
//				if(isfinished == false)
//					stopAffair(true);
			} else { // hidden ssid
				mWifiConf.hidden = true;
				
				if(mWifiConf.passwd.isEmpty()){
					mWifiConf.type = "nopass";
					connectWifi(mWifiConf);
					mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, 1000 * 15);
				}else{
					// 	TODO try the security one by one: wpa, wep
					mHandler.sendEmptyMessage(MSG_TRY_WPA);
				}
			}
		}
	};
	
	private boolean connecting = false;
	private IntentFilter connectFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
	private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if(NetworkInfo.State.CONNECTED.equals(info.getState()) ) {
				Robot.getInstance().getContext().unregisterReceiver(this);
				connecting = false;
				Log.d(NAME, "WiFi 连接成功");				

				if(isfinished == false)
					stopAffair(true);
			}
		}
	};
	
	/* {"ssid":"WIFI名称","password":"WIFI密码"} */
	private BarCodeService.ResultCallback resultCB = new BarCodeService.ResultCallback() {
		@Override
		public void onReadfinished(String[] result) {
			Log.d(NAME, "BarCode: " + result[0]);
			
			if(null == result[0] || result[0].isEmpty()){
				Log.d(NAME, "无法识别的wifi二维码");
				if(isfinished == false)
					stopAffair(true);
				return;
			}			

			
			try {
				byte[] configInfo = Base64.decode(result[0], Base64.DEFAULT);
				Log.d(NAME, "config info: " + new String(configInfo));
				
				JSONObject json = new JSONObject(new String(configInfo));
				String ssid = json.getString("ssid");
				String passwd = json.getString("password");
				
				if(null == ssid || ssid.isEmpty()){
					Log.e(NAME, "ssid formate error");
					if(isfinished == false)
						stopAffair(true);
					return;
				}
				
				mWifiConf = new WifiConfig();
				mWifiConf.ssid = ssid;
				mWifiConf.passwd = passwd;
				mWifiConf.hidden = false;
				mWifiConf.type = "unknown";
				
				// scan to get detail info about the wifi, then conn
				Robot.getInstance().getContext().registerReceiver(scanReceiver, scanFilter);
				scanning = true;
				mWifi.startScan();
				
			} catch (JSONException e) {
				e.printStackTrace();
				Log.e(NAME, "json format error");
				if(isfinished == false)
					stopAffair(true);
				return;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				Log.e(NAME, "base64 format error");
				if(isfinished == false)
					stopAffair(true);
			}
				
		}
	};
	
	private BarCodeService.ErrorCallback errorCB = new BarCodeService.ErrorCallback() {
		@Override
		public void onReadError(int err) {
			Log.d(NAME, "超时退出");
			if(isfinished == false)
				stopAffair(true);
		}
	};
	
	@Override
	public String name() {
		return NAME;
	}

	public SetWiFi(RobotAffairManager am) {
		super(am);
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		isfinished = false;
		Log.d(NAME, "开始设置为wifi");
		mHandler = new Handler(){
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_TRY_WPA:
					mWifiConf.type = "WPA";
					connectWifi(mWifiConf);
					mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, 1000 * 15);
					break;
				case MSG_TRY_WEP:
					mWifiConf.type = "WEP";
					connectWifi(mWifiConf);
					mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT, 1000 * 15);
					break;
				case MSG_TIMEOUT:
					if(connecting){
						Robot.getInstance().getContext().unregisterReceiver(connectReceiver);
						connecting = false;
					}
					if(!mWifiConf.hidden){
						Log.d(NAME, "连接  wifi 超时");
						if(isfinished == false)
							stopAffair(true);
					}else{
						if("nopass".equals(mWifiConf.type) ){
							Log.d(NAME, "连接  wifi 超时: hidden, nopass");
							if(isfinished == false)
								stopAffair(true);
						}else if("WPA".equals(mWifiConf.type)){
							mHandler.sendEmptyMessage(MSG_TRY_WEP);
						}else{ // WEP
							Log.d(NAME, "连接  wifi 超时: hidden");
							if(isfinished == false)
								stopAffair(true);
						}
					}
					break;
				default:
					break;
				}
			};
		};
		
		mWifi = (WifiManager) Robot.getInstance().getContext().getSystemService(Context.WIFI_SERVICE);
		mWifi.setWifiEnabled(true);
		barCode = (BarCodeService) Robot.getInstance().getService(BarCodeService.NAME);
		if(!barCode.startReading(resultCB, errorCB)) {
			Log.d(NAME, "启动摄像头失败");
			// show tips .
			if(isfinished == false)
				stopAffair(true);
		}
	}
	
	@Override
	protected void onFinished() {
		isfinished = true;
		mHandler.removeMessages(MSG_TIMEOUT);

		barCode.stopReadingByhandle();
		try {
			if(scanning)	Robot.getInstance().getContext().unregisterReceiver(scanReceiver);
			if(connecting)	Robot.getInstance().getContext().unregisterReceiver(connectReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		super.onFinished();
		RobotDebug.d(NAME, "停止wifi事务");
	}

}
