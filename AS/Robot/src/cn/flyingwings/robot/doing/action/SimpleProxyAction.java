/**
 * SimpleProxyAction.java 2016-7-13
 */
package cn.flyingwings.robot.doing.action;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author WangBaoming
 *
 */
public class SimpleProxyAction extends RobotAction {
	public final static String NAME = "simple_query_proxy";
	
	private static Map<String, Class<?>> sQueryAction = new HashMap<String, Class<?>>();
	static {
		sQueryAction.put(SmartDevice.NAME, SmartDevice.class);
		sQueryAction.put(InfraredAction.NAME, InfraredAction.class);
		sQueryAction.put(MoodinfoAction.NAME, MoodinfoAction.class);
		sQueryAction.put(PathLearnAction.NAME, PathLearnAction.class);
		sQueryAction.put(ReminderAction.NAME, ReminderAction.class);
		sQueryAction.put(MusicPlayAction.NAME, MusicPlayAction.class);
	}
	
	private RobotAction realAction = null;

	@Override
	public int type() {
		return ACTION_TYPE_SIMPLE;
	}

	@Override
	public String name() {
		return NAME;
	}
	
	/**
	 * <pre>
	 * eg:
	 * {
	 * 	"name": "simple_query_proxy",
	 * 	"realAction": {
	 * 		"subName": "inquiry",
	 * 		"species": "env_sensor",
	 * 		"name": "smart_device",
	 * 		"opcode": 92402,
	 * 		"mode": "type"
	 * 	}
	 * }
	 * <pre>
	 */
	@Override
	public boolean parse(String s) {
		super.parse(s);
		
		// parse action string to fetch the real action
		try {
			JSONObject jobj = new JSONObject(s);

			JSONObject jRealAction = jobj.getJSONObject("realAction");
			String realActionName = jRealAction.getString("name");
			
			if(sQueryAction.containsKey(realActionName)){
				Class<RobotAction> actionClass = (Class<RobotAction>) sQueryAction.get(realActionName);
				
				realAction = actionClass.newInstance();
				
				return realAction.parse(jRealAction.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public void doing() {
		super.doing();
		if(null != realAction){
			realAction.doing();
		}
	}

}
