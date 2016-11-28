package cn.flyingwings.robot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.UserStrategy;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import cn.flyingwings.robot.FaceService.FaceData;
import cn.flyingwings.robot.FaceService.XmlDomService;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.xmppservice.XmppService;

public class MyApplication extends Application {

	public static Map<String, FaceData> mFaceAll = new HashMap<String, FaceData>();

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		initFaceData();
		
		StringBuffer param = new StringBuffer();
		param.append("appid=" + getString(R.string.app_id));
		param.append(",");
		// 设置使用v5+
		param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
		SpeechUtility.createUtility(this, param.toString());
		Setting.setShowLog(false);
		Setting.setLogPath("/mnt/sdcard/xunfei/");
		// 异常捕获
	    CrashHandler.getInstance().init(this);
		buglyInit();
			
		DataStatisticsService.power_on_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	private void buglyInit() {
		UserStrategy userStrategy = new UserStrategy(this);

		String versionName = "";
		String packageName = getPackageName();
		try {
			PackageManager pm = getPackageManager();
			PackageInfo pinfo = pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
			versionName = pinfo.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(!TextUtils.isEmpty(XmppService.robot_SN)){
			userStrategy.setDeviceID(XmppService.robot_SN);
		}
		userStrategy.setAppVersion(versionName);
		userStrategy.setAppPackageName(packageName);
		userStrategy.setAppReportDelay(20000);

		CrashReport.initCrashReport(this, getString(R.string.bugly_id), true, userStrategy);
	}

	
	private void initFaceData(){
		try {
			String[] face = getAssets().list("resXml");
			for(int i = 0; i < face.length; i++){
				mFaceAll.put(face[i].replace(".xml", ""), XmlDomService.parseData("resXml/"+ face[i], this));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
