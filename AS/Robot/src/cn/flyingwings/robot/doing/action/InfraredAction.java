/**
 * InfraredAction.java 2016-4-28
 */
package cn.flyingwings.robot.doing.action;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.json.util.GsonInstance;
import cn.flyingwings.robot.smartdevice.infrared.InfraredDataDefine;
import cn.flyingwings.robot.smartdevice.infrared.InfraredDataDefine.RemoteControllerType;
import cn.flyingwings.robot.smartdevice.infrared.InfraredDevice;
import cn.flyingwings.robot.smartdevice.infrared.InfraredService;
import cn.flyingwings.robot.smartdevice.infrared.RemoteController;
import cn.flyingwings.robot.smartdeviceservice.DBHelper;
import cn.flyingwings.robot.smartdeviceservice.ZigbeeService;
import cn.flyingwings.robot.utils.StringUtil;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * @author WangBaoming
 *
 */
public class InfraredAction extends RobotAction {
	public final static String NAME = "infrared";
	
	private JSONObject mParams = null;
	
	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	public boolean parse(String s) {
		super.parse(s);
		try {
			JSONObject jobj = new JSONObject(s);
			mParams = jobj; 
			mParams.remove("name");
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	@Override
	public void doing() {
		super.doing();
		
		InfraredService infraredService = (InfraredService) Robot.getInstance().getService(InfraredService.NAME);
		XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
		
		Gson gson = GsonInstance.get();
		try {	          			
			int opcode = mParams.getInt("opcode");
			switch (opcode) {	
			case TransferCode.infrared_Forward_Add_Del_Modify:{
				String opt = mParams.getString("opt");
				
				InfraredDevice device = gson.fromJson(mParams.getString("device"), InfraredDevice.class);
				
				if("add".equals(opt)){
					device.setAddedTime(StringUtil.getCurrentTimeString());
					boolean successful = infraredService.addNewInfraredDevice(device);
					if(successful){
						mParams.put("result", 0);
					}else{
						mParams.put("result", 1);
						mParams.put("error_msg", "设备已经存在");
					}
				}else if("del".equals(opt)){
					infraredService.delInfraredDevice(device);
					mParams.put("result", 0);
					
					removeFromRecentUse(device);
				}else if("update".equals(opt)){
					infraredService.updateInfraredDevice(device);
					mParams.put("result", 0);
				}else{
					Log.e(NAME, "infrared device #not support operation: " + opt);
				}
				
				Log.i(NAME, "opt: " + opt + "\nmsg: " + mParams.toString());

  				xmppservice.sendMsg("R2C", mParams.toString(), opcode);	
				break;
			}
          	case TransferCode.infrared_Forward_Inquire :{
          		String mode = mParams.getString("mode");
          		if("all".equals(mode)){
          			ArrayList<InfraredDevice> infraredDevices = infraredService.getAllInfraredDevices();
          			mParams.put("devices", new JSONArray(infraredDevices.toString()) );
          		}else if("device".equals(mode)){
          			String strIeeeaddr = mParams.getString("ieeeaddr");
          			strIeeeaddr = strIeeeaddr.replace("-", "");
          			long ieeeaddr = Long.valueOf(strIeeeaddr, 16);
          			
          			InfraredDevice device = infraredService.getInfraredDevice(ieeeaddr);
          			ArrayList<InfraredDevice> devices = new ArrayList<InfraredDevice>(1);
          			if(null != device){
          				devices.add(device);
          			}
          			
          			mParams.put("devices", new JSONArray(devices.toString()) );
          		}
          		
				Log.i(NAME, "msg: " + mParams.toString());

  				xmppservice.sendMsg("R2C", mParams.toString(), opcode);	
				break;
          	}
          	case TransferCode.remote_Control_Add_Del_Modify:{
				String opt = mParams.getString("opt");
				RemoteController remoteController = gson.fromJson(mParams.getString("remote"), RemoteController.class);
				
				if("add".equals(opt)){
					remoteController.addedTime = StringUtil.getCurrentTimeString();
					
					if(StringUtil.isNullOrEmpty(remoteController.location) || StringUtil.isNullOrEmpty(remoteController.name)) {
						mParams.put("result", 1);
						mParams.put("error_msg", "遥控器位置和名称不能为空");
					} else {
						if(isNameExistedInOther(remoteController.location, remoteController.name)){
							mParams.put("result", 1);
							mParams.put("error_msg", "设备名称已存在");
						}else{
							// name uniqueness check
							boolean nameExisted = infraredService.isRemoteControllerExisted(remoteController.location, remoteController.name);
							if(nameExisted){
								mParams.put("result", 1);
								mParams.put("error_msg", "遥控器名称重复");
							}else{						
								boolean successful = infraredService.addNewRemoteController(remoteController);
								if(successful){
									mParams.put("result", 0);
									mParams.put("remote", new JSONObject(remoteController.toString()));
								}else{
									mParams.put("result", 1);
									mParams.put("error_msg", "添加遥控器失败");
								}
							}
						}						
					}
					
				}else if("del".equals(opt)){
					infraredService.delRemoteController(remoteController);
					mParams.put("result", 0);
					
					// remove from recent use
					removeFromRecentUse(remoteController);
				}else if("update".equals(opt)){					
					RemoteController rc = infraredService.getRemoteController(remoteController.ieeeaddr, remoteController.rid);
					if(null == rc){
						Log.e(NAME, "no such rc");
						return ;
					}else{
						if(isNameExistedInOther(remoteController.location, remoteController.name)){
							mParams.put("result", 1);
							mParams.put("error_msg", "设备名称已存在");
						}else{
							if(rc.name.equals(remoteController.name) && rc.location.equals(remoteController.location)){ // nothing changed
								mParams.put("result", 0);
							}else{
								boolean nameExisted = infraredService.isRemoteControllerExisted(remoteController.location, remoteController.name);
								if(nameExisted){
									mParams.put("result", 1);
									mParams.put("error_msg", "遥控器名称重复");
								}else{
									infraredService.updateRemoteController(remoteController);
									mParams.put("result", 0);
									
									updateRecentUseDevice(remoteController);
								}
							}
						}
					}
				}else{
					Log.e(NAME, "remote controller #not support operation: " + opt);
				}


				Log.i(NAME, "remote controller #opt: " + opt + "\nmsg: " + mParams.toString());

  				xmppservice.sendMsg("R2C", mParams.toString(), opcode);	
				break;
          	}
          	case TransferCode.remote_Control_Inquire:
          		// TODO stub, latter may be used.
				Log.i(NAME, "do nothing #msg: " + mParams.toString());
				break;
          	case TransferCode.remote_Control_Key_Send:{
          		// ieeeaddr
      			String strIeeeaddr = mParams.getString("ieeeaddr");
      			strIeeeaddr = strIeeeaddr.replace("-", "");
      			// FIXME exception if the highest bit is 1, such as "AC59B367CE9C3871". 1 means the sign, minus.
      			long ieeeaddr = Long.valueOf(strIeeeaddr, 16);
      			
      			// type
      			RemoteControllerType type = gson.fromJson(mParams.getString("species"), RemoteControllerType.class);
      			
      			// index
      			int index = mParams.getInt("index");
      			
      			// key
      			int key = mParams.getInt("key_num"); 

      			// rid
      			long rid = mParams.optLong("rid", -1);

      			// data
//      			int data = mParams.optInt("key_value");
      			byte[] data = null;
      			JSONObject ret = new JSONObject();
      			ret.put("opcode", String.valueOf(opcode));
      			if(RemoteControllerType.AIRCONDITION.equals(type)){ // air conditioner
      				JSONArray values = mParams.getJSONArray("values");
          			
          			int len = values.length();
          			if(InfraredDataDefine.AIR_CONDITIONER_KEY_PARAM_LEN - 1 != len){
              			ret.put("result", 1);
              			ret.put("error_msg", "参数错误");
              			
        				Log.i(NAME, "msg: " + mParams.toString() + "\nresponse: " + ret.toString());
          				xmppservice.sendMsg("R2C", ret.toString(), opcode);	
          				break;
          			}else{
          				data = new byte[InfraredDataDefine.AIR_CONDITIONER_KEY_PARAM_LEN];
              			for(int i = 0; i < len; i++){
              				data[i] = (byte) values.getInt(i);
              			}
              			
              			// insert the key name
              			data[6] = data[5];
              			data[5] = (byte) key;              			
          			}
      			}else{ // other
      				data = new byte[0];
      			}

          		boolean successful = infraredService.sendInfraredKeyCode(ieeeaddr, type.value(), index, key, data);
          		if(successful){
          			ret.put("result", 0);
          			
          			if(-1 != rid){ // update/(added to) recent use table
              			RemoteController rc = infraredService.getRemoteController(ieeeaddr, rid);
              			if(null != rc) increaseRecentUseTimes(rc);
          			}
          		}else{
          			ret.put("result", 1);
          			ret.put("error_msg", "发送失败");
          		}

				Log.i(NAME, "msg: " + mParams.toString() + "\nresponse: " + ret.toString());

  				xmppservice.sendMsg("R2C", ret.toString(), opcode);	
				break;
          	}
          	case TransferCode.infrared_Version_Inquire:
      			JSONObject ret = new JSONObject();
      			ret.put("opcode", String.valueOf(opcode));
      			ret.put("version", InfraredService.VERSION);

				Log.i(NAME, "msg: " + mParams.toString() + "\nresponse: " + ret.toString());
  				xmppservice.sendMsg("R2C", ret.toString(), opcode);	
				break;

			default:
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isNameExistedInOther(String location, String name){
		ZigbeeService zgbService = (ZigbeeService) Robot.getInstance().getService(ZigbeeService.NAME);
		return zgbService.isExistedInWhiteDevice(location, name);
	}

	private void removeFromRecentUse(InfraredDevice infraredDevice) {
		try {
			String ieeaddr = new JSONObject(infraredDevice.toString()).getString("ieeeaddr");
			DBHelper.getInstance().delDevice_recent_use(ieeaddr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void removeFromRecentUse(RemoteController rc) {
		try {
			String ieeaddr = new JSONObject(rc.toString()).getString("ieeeaddr");
			DBHelper.getInstance().delDevice_recent_use(ieeaddr, rc.rid);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void updateRecentUseDevice(RemoteController rc) {
		try {
			String ieeeaddr = new JSONObject(rc.toString()).getString("ieeeaddr");
			DBHelper.getInstance().updateDevice_recent_use(
					"infrared", 
					rc.name,
					ieeeaddr, 
					rc.location, 
					null, 
					-1,
					rc.rid);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void increaseRecentUseTimes(RemoteController rc) {
		try {
			String ieeeaddr = new JSONObject(rc.toString()).getString("ieeeaddr");
			JSONObject recentInfo = DBHelper.getInstance().inquiryDevRecentUse(ieeeaddr, rc.rid);
			
			if(null != recentInfo){
				int usedTimes = recentInfo.optInt(DBHelper.DEV_RECENT_USE_TIMES, 1);
				usedTimes++;
				
				DBHelper.getInstance().updateDevice_recent_use(
						"infrared", 
						rc.name,
						ieeeaddr, 
						rc.location,
						null,
						usedTimes,
						rc.rid);
				
				return ;
			}

			DBHelper.getInstance().insertdevice_recent_use(
					"infrared", 
					rc.name,
					ieeeaddr, 
					rc.addedTime,
					rc.location,
					null,
					1,
					"",
					rc.rid);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
