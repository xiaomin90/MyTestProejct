package cn.flyingwings.robot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

import android.text.TextUtils;
import cn.flyingwings.robot.chatService.ThreadPoolDo;
import cn.flyingwings.robot.chatService.TuRingUtils.WebCallBack;
import cn.flyingwings.robot.json.util.GsonInstance;
import cn.flyingwings.robot.smartdeviceservice.SmartDevLog;

public class RobotServerURL {

	public static final String TAG = "RobotHttp";
	public static AppMode appMode = AppMode.develop;
	public static String urlPrefix = "";

	enum AppMode {
		release, // 发布模式
		develop, // 开发模式
		test // 测试模式
	}

	public static String XmppHttp;
	public static int XmppHttp_PORT;
	public static String APPRTC_HTTP;
	public static String Update_HTTP;
	public static String Alarm_HTTP;
	public static String Voice_Log_HTTP;
	public static String AliLog_HTTP;
	public static String LogRecord_HTTP;
	public static String Update_Process_HTTP;

	static {
		switch (appMode) {
		case release:
			XmppHttp = "im.flwrobot.com";// 正式服务器
			XmppHttp_PORT = 5222;// 正式服务器端口
			APPRTC_HTTP = "https://rtc1.flwrobot.com";//
			Update_HTTP = "https://update.flwrobot.com/robot/version.do";// 正式服务器
			Alarm_HTTP = "https://implus.flwrobot.com/robot/alarm.do";// 正式服务器
			urlPrefix = "https://implus.flwrobot.com/robot/";
			AliLog_HTTP = "https://implus.flwrobot.com/robot/oss/token.do"; // 正式服务器LOG
			LogRecord_HTTP = "https://implus.flwrobot.com/robot/system/log.do";// 正式服务器LOG_REcord
			Update_Process_HTTP = "https://implus.flwrobot.com/robot/version/update/progress.do"; // 正式服务器升级接口
			break;
		case develop:
			XmppHttp = "imdev.flwrobot.com";// 开发服务器
			XmppHttp_PORT = 35222; // 开发服务器端口
			APPRTC_HTTP = "https://rtcdev.flwrobot.com";// 开发服务器
			Update_HTTP = "https://updatedev.flwrobot.com/robot/version.do";// 开发服务器
			Alarm_HTTP = "https://implusdev.flwrobot.com/robot/alarm.do";// 开发服务器
			urlPrefix = "https://implusdev.flwrobot.com/robot/";
			AliLog_HTTP = "https://implusdev.flwrobot.com/robot/oss/token.do"; // 开发服务器 LOG
			LogRecord_HTTP = "https://implusdev.flwrobot.com/robot/system/log.do"; // 开发服务器 LOG_REcord
			Update_Process_HTTP = "https://implusdev.flwrobot.com/robot/version/update/progress.do"; // 开发服务器升级接口
			break;
		case test:
			XmppHttp = "imtest.flwrobot.com";// 测试服务器
			XmppHttp_PORT = 5222; // 测试服务器
			APPRTC_HTTP = "https://rtctest.flwrobot.com";// 测试服务器
			Update_HTTP = "https://updatetest.flwrobot.com/robot/version.do";// 测试服务器
			Alarm_HTTP = "https://implustest.flwrobot.com/robot/alarm.do";// 测试服务器
			urlPrefix = "https://implustest.flwrobot.com/robot/";
			AliLog_HTTP = "https://implusdev.flwrobot.com/robot/oss/token.do"; // 测试服务器 LOG 暂时使用开发
			LogRecord_HTTP = "https://implusdev.flwrobot.com/robot/system/log.do"; // 测试服务器 LOG_REcord 暂时使用开发
			Update_Process_HTTP = "https://implusdev.flwrobot.com/robot/version/update/progress.do"; // 开发服务器升级接口
			break;
		}
	}

	public static final String ActionAffairPath = "robot.clf";

	public enum HttpUrl {
		versionDo, alarmDo, voiceLogDo
	}

	public enum HttpMethod {
		post, get
	}

	public static Map<HttpUrl, String> urlMap = new HashMap<HttpUrl, String>();
	public static boolean devMode = true;

	static {
		urlMap.put(HttpUrl.versionDo, "version.do");
		urlMap.put(HttpUrl.alarmDo, "alarm.do");
		urlMap.put(HttpUrl.voiceLogDo, "speech/log.do");
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param clazz
	 * @param callBack
	 */
	public static <T> void doRequest(final HttpUrl enumUrl, final Map<String, String> params, final HttpMethod method,
			final Class<T> clazz, final WebCallBack callBack) {

		ThreadPoolDo.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				BufferedReader reader = null;
				HttpURLConnection connection = null;
				String url = urlPrefix + urlMap.get(enumUrl);
				try {
					String strParams = "";
					if (params != null) {
						strParams = mapToParams(params);
					}
					if (method == null) {
						throw new IllegalArgumentException("请选择http请求方式");
					}
					if (method == HttpMethod.get) {
						if (!TextUtils.isEmpty(strParams)) {
							connection = (HttpURLConnection) new URL(url + "?" + strParams).openConnection();
						} else {
							connection = (HttpURLConnection) new URL(url).openConnection();
						}
						connection.setRequestMethod("GET");
						connection.setDoInput(true);
						connection.setConnectTimeout(5000);
						connection.setReadTimeout(5000);
						connection.setDoOutput(true);
						connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					} else if (method == HttpMethod.post) {
						connection = (HttpURLConnection) new URL(url).openConnection();
						connection.setRequestMethod("POST");
						connection.setUseCaches(false);
						connection.setDoInput(true);
						connection.setConnectTimeout(5000);
						connection.setReadTimeout(5000);
						connection.setDoOutput(true);
						connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						connection.setFixedLengthStreamingMode(strParams.getBytes().length);
						// Send POST request.
						OutputStream outStream = connection.getOutputStream();
						outStream.write(strParams.getBytes());
						outStream.close();
					}
					connection.connect();

					// Get response.
					int responseCode = connection.getResponseCode();
					if (responseCode != 200) {
						connection.disconnect();
						SmartDevLog.e(TAG, "response #error code : " + responseCode);
					}

					reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
					StringBuilder builder = new StringBuilder();
					String line = "";
					builder.append(line);
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
					connection.disconnect();
					if (clazz != null) {
						String datas = builder.toString();
						Gson gson = GsonInstance.get();
						T bean = gson.fromJson(datas, clazz);
						if (callBack != null)
							callBack.onSuccess(bean);
					}

				} catch (SocketTimeoutException e) {
					SmartDevLog.e(TAG, "response time out : " + e.toString());
				} catch (IOException e) {
					SmartDevLog.e(TAG, "response IO error : " + e.toString());
				} finally {
					if (reader != null)
						try {
							reader.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		});

	}

	public static String mapToParams(Map<String, String> params) {
		Set<Entry<String, String>> set = params.entrySet();
		StringBuilder mBuilder = new StringBuilder();
		for (Entry<String, String> item : set) {
			mBuilder.append(item.getKey());
			try {
				mBuilder.append("=");
				/*if (item.getKey().equals("session_id") || item.getKey().equals("version")) {
					mBuilder.append(item.getValue());
				} else {
					mBuilder.append(URLEncoder.encode(item.getValue(), "UTF-8"));
				}*/
				
				mBuilder.append(URLEncoder.encode(item.getValue(), "UTF-8"));
				mBuilder.append("&");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return mBuilder.deleteCharAt(mBuilder.lastIndexOf("&")).toString();
	}

}
