package cn.flyingwings.robot.doing.event;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.doing.action.RobotDivide;
import cn.flyingwings.robot.doing.action.SeekHome;
import cn.flyingwings.robot.doing.affair.PathRecurAffair;
import cn.flyingwings.robot.motion.MotionLog;
import cn.flyingwings.robot.motion.MoveDect;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.CANService.CANMessage;


/**
 *  client_change  客户端连接状态变化，需要机器人做出相应的处理：
 *  1.用户离线之后，需要释放webrtc连接
 *  2.修改client连接状态
 *  3.修改其他相关事务触发需要做的事。
 * @author min.js
 * time 2016-1-11
 */
public class ClientChange extends RobotEvent{

	public static final String NAME = "client_change";
	private String TAG = "client_change";
	private String KEY_NAME = "name";
	private String KEY_DATA = "data";
	private String KEY_STATUS = "status";
	private String[] client_status = {"online","offline"};
	public static final String ACTION_DisConnect = "{\"name\":\"client_control\",\"todo\":\"0\",\"session_mode\":\"0\",\"apprtc_server\":\"0\",\"room_id\":\"0\",\"state\":\"disconnect\"}";
	
	@Override
	public String name() {
		
		return NAME;
	}

	@Override
	public void map(RobotDivide divider, Intent intent) {

		String name = intent.getStringExtra(KEY_NAME);
		String data = intent.getStringExtra(KEY_DATA);
		if(name.equals(NAME)){
			JSONObject data_Json ;
			try {
				data_Json = new JSONObject(data);
			} catch (JSONException e) {
				e.printStackTrace();
				RobotDebug.d(TAG," data is not json format.");
				return;
			}
			if(data_Json.isNull(KEY_STATUS)){
				RobotDebug.d(TAG," data is not contains status.");
				return;
			}
			
			try {
				if(data_Json.getString(KEY_STATUS).equals(client_status[0])){
					// client online
					/**
					 * 停止运动  停止路径复现，停止一些运动,如果处于找充电座不停止。
					 */
					String currentAffair = Robot.getInstance().getManager().getCurrentAffair();
					boolean isSeekHome  =  ( (currentAffair != null) && (currentAffair.equals(SeekHome.NAME)));
					if(!isSeekHome) {
						PathService pathserver = (PathService)Robot.getInstance().getService(PathService.NAME);
						pathserver.cancelRecurPath();
						byte[] data2 = new byte[0];
						CANMessage msg = new CANMessage(0x120, 0x0, data2);// header.
						CANMessage msg2 = new CANMessage(0x100, 0x0, data2);// body
						CANService service = (CANService) Robot.getInstance().getService(CANService.NAME);
						service.send(msg);
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						service.send(msg2);
					}
				}
				else if(data_Json.getString(KEY_STATUS).equals(client_status[1])){
					// client offline
					RobotDebug.d(TAG, "disconnect Apprtc。");
					Robot.getInstance().getManager().toDo(ACTION_DisConnect);
					/**
					 * 停止运动  停止路径复现，停止一些运动,如果处于找充电座不停止。
					 */
					String currentAffair = Robot.getInstance().getManager().getCurrentAffair();
					boolean isSeekHome  =  ( (currentAffair != null) && (currentAffair.equals(SeekHome.NAME)));
					if(!isSeekHome) {
						PathService pathserver = (PathService)Robot.getInstance().getService(PathService.NAME);
						pathserver.cancelRecurPath();
						byte[] data2 = new byte[0];
						CANMessage msg = new CANMessage(0x120, 0x0, data2);// header.
						CANMessage msg2 = new CANMessage(0x100, 0x0, data2);// body
						CANService service = (CANService) Robot.getInstance().getService(CANService.NAME);
						service.send(msg);
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						service.send(msg2);
					}
				}
				else{
					RobotDebug.d(TAG, "data status is not correct : " + data_Json.getString(KEY_STATUS));
					return;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				RobotDebug.d(TAG, "data status error :" + e.toString());
				return;
			}			
		}
		return;
	}

	
}
