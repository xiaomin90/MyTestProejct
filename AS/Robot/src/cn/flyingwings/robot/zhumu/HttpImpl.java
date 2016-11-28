package cn.flyingwings.robot.zhumu;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpImpl implements Constants{
	static Logs log = new Logs("us.zm.sdkexample.HttpUtils",
			Logs.DebugType.D);

	public static Meeting httpWithPost() {
		/* ����HTTP Post���� */
		String url = "https://api.zhumu.me/v1/user/get";
		HttpPost httpRequest = new HttpPost(url);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("api_key", API_KEY));
		params.add(new BasicNameValuePair("api_secret", API_SECRET));
		params.add(new BasicNameValuePair("logintype", "3"));
		params.add(new BasicNameValuePair("loginname", "SDKTest@zhumu.me"));

		try {

			// ����HTTP request
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			// ȡ��HTTP response
			// HttpResponse httpResponse=new DefaultHttpClient().execute(httpRequest);
			HttpResponse httpResponse = getNewHttpClient().execute(httpRequest);

			// ��״̬��Ϊ200 ok
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				// ȡ����Ӧ�ִ�
				String strResult = EntityUtils.toString(httpResponse
						.getEntity());
				log.D("httpWithPost()  strResult=" + strResult);

				// {"code":100,"zcode":16457481,"id":"wqljsh1JSGGL1ji_40UarQ","username":"???","mobile":null,"usertype":0,"det":null,
				// "createtime":"\/Date(1430378900000)\/","createby":"fulq@yonyou.com","pmi":"15449593466","role":0,
				// "email":"fulq@yonyou.com","isowner":0,"token":"qmzg0uXIeSS69lCTo8UTQdjVh8EtVvi_z-DVGXEPFHM.BgIgMVZRc3VBSW9nTFVPVjRpZnY4aC9id2xrMTVBNm45OWNANTZhODFiNjE5NWFlOWQ5YTc2NTYzOTdmNGYxZjk3YWUxNjcwMmUwODk1MDEyOWNlNjNiNDViMmM3N2FkZDJlZgA"}
				JSONObject jsonObject = new JSONObject(strResult.toString());
				int code = jsonObject.getInt("code");
				int zcode = jsonObject.getInt("zcode");
				String id = jsonObject.getString("id");// User_id

				String username = jsonObject.getString("username");
				String mobile = jsonObject.getString("mobile");
				int usertype = jsonObject.getInt("usertype");
				String det = jsonObject.getString("det");
				String createtime = jsonObject.getString("createtime");
				String createby = jsonObject.getString("createby");

				String pmi = jsonObject.getString("pmi");
				int role = jsonObject.getInt("role");
				String email = jsonObject.getString("email");
				int isowner = jsonObject.getInt("isowner");
				int accounttype = jsonObject.getInt("accounttype");
				String token = jsonObject.getString("token");

				Meeting meet = new Meeting();
				meet.setCode(code);
				meet.setZcode(zcode);
				meet.setId(id);
				meet.setUsername(username);
				meet.setMobile(mobile);
				meet.setUsertype(usertype);
				meet.setDet(det);
				meet.setCreatetime(createtime);
				meet.setCreateby(createby);
				meet.setPmi(pmi);
				meet.setRole(role);
				meet.setEmail(email);
				meet.setIsowner(isowner);
				meet.setAccounttype(accounttype);
				meet.setToken(token);
				return meet;
			} else {
				log.W("httpWithPost() error getStatusCode ="+httpResponse.getStatusLine().getStatusCode());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

}
