package cn.flyingwings.robot.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.google.gson.Gson;

import cn.flyingwings.robot.chatService.ThreadPoolDo;
import cn.flyingwings.robot.chatService.TuRingUtils.WebCallBack;

public class MusicGet {

	// public static String url = "https://test.flwrobot.com/svc-music/music/playlist.do";

	public static String url = "https://ting.flwrobot.com/music/playlist.do";

	/**
	 * 
	 * @param singer
	 * @param songName
	 * @param sRSession 登录后服务器分配给机器人的id
	 * @param webCallBack
	 */
	public static void doRequest(String singer, String songName, String sRSession, final WebCallBack webCallBack) {
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("limit", "20"));
		params.add(new BasicNameValuePair("singer", singer));
		params.add(new BasicNameValuePair("song_name", songName));
		params.add(new BasicNameValuePair("sr_session", sRSession));
		ThreadPoolDo.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				getMusicRes(params, webCallBack);
			}
		});
	}

	public static void getMusicRes(List<NameValuePair> parmas, WebCallBack webCallBack) {
		String result = null;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);
		HttpResponse httpResponse = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(parmas, HTTP.UTF_8));
			// 发送HttpPost请求，并返回HttpResponse对象
			httpResponse = httpClient.execute(post);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				result = EntityUtils.toString(httpResponse.getEntity());
				JSONObject jsonData = new JSONObject(result);
				if (jsonData.getString("result").equals("0")) {
					Gson gson = new Gson();
					MusicBean musicBean = gson.fromJson(result, MusicBean.class);
					webCallBack.onSuccess(musicBean);
				} else {
					webCallBack.onError();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			webCallBack.onError();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

	}

}
