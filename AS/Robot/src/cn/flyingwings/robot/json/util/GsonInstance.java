/**
 * GsonInstance.java 2016-4-28
 */
package cn.flyingwings.robot.json.util;

import com.google.gson.Gson;

/**
 * hold the {@link Gson} instance.
 * @author wangbaoming
 *
 */
public class GsonInstance {
	private final static Gson sGson = new Gson();
	
	public static Gson get(){
		return sGson;
	}
}
