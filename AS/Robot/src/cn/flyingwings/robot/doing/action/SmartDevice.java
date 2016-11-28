package cn.flyingwings.robot.doing.action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.smartdevice.infrared.InfraredService;
import cn.flyingwings.robot.smartdeviceservice.Dev433Service;
import cn.flyingwings.robot.smartdeviceservice.Device;
import cn.flyingwings.robot.smartdeviceservice.SmartDevLog;
import cn.flyingwings.robot.smartdeviceservice.ZigbeeService;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

/**
 * 智能外设Action
 * 用于语音操作:
 *  {"name":"smart_device","from":"voice","device":"xxx","operation":"xxx","place":"xxx","env":"true"}
 *  env 为true时，表示查询环境，其余项可添为空.env为false时，表示是其他智能外设的操作
 *  {"name":"smart_device","sercurity":"on","from":"voice"}
 *  sercurity 为on时，表示设防 ，sercurity为off时，表示撤防
 * @author Administrator
 *
 */
public class SmartDevice   extends RobotAction {
	private final static String TAG = "smart_device";
	public final static String NAME = "smart_device";
	
	private static final String[] inquire_mode = {
		"all",  //  all devices 
		"type", //  a type devices
		"device", //  a device
	};
	private static final String[]  Dev433_species = {
		"door_contact", /*door */
		"ir_detection",   /*IR */
		"smoke_sensor", /*smoke*/
		"gas_sensor",     /*gas*/
		"emergency_sensor", /*emergency*/
	};
	
	private  int    opcode = 0 ;
	// zigbeeDev_Add_Del_Update opt device
	private  String opt = null;
	private  int    count = 0;
	private  JSONArray device_array = null;
	private  JSONObject device = null;
	// zigbeeDev_Inquire mode species ieeeaddr endpointid name
	private  String mode = null;
	private  String species = null;
	private  String ieeeaddr = null;
	private int endpointid = -1;
	private String devname = null;
	// zigbeeDev_Control is  opt  species device
	// zigbeeDev_Recent_Devlist Opt 
    // zigbeeDev_History_Data species	time 	device
	private String time = null;
	private int index = 0 ;
	// dev433 
	private int addr = 0;
	
	/**
	 * for voice.
	 */
	private String fromvoice = null;
	private String device_name = null;
	private String operation = null;
	private String place = null;
	private int	quantity = -1;
	private boolean env = false;
	
	/**
	 * 撤防设防
	 */
	private String sercurity = null;
	private String ser_formvoice = null;
	
	public boolean isDev433(String type)
	{
		for(int i = 0 ; i < Dev433_species.length; i++)
		{
			if(Dev433_species[i].equals(type))
				return true;
		}
		return false;
	}
	
	
	private void resetData()
	{
		opcode   = 0;
		opt      = null;
		count    = 0;
		device_array = null;
		device   = null;
		mode     = null;
		species  = null;
		ieeeaddr = null;
		endpointid = -1;
		devname = null;
		time = null;
		addr  = 0;
		index = 0;
		fromvoice = null;
		device_name = null;
		operation = null;
		place = null;
		env = false;
		sercurity = null;
		ser_formvoice = null;
	}
	
	
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
		/**
		 * 重置数据
		 */
		RobotDebug.d(TAG, "parse  : " + s);
		resetData();
		JSONObject data = null;
		try {
			data = new JSONObject(s);
		} catch (JSONException e) {
			RobotDebug.d(TAG, "parse smartDevice String not json format.");
			return false;
		}
		
		if(!data.isNull("sercurity"))
		{
			try {
				sercurity = data.getString("sercurity");
				ser_formvoice = data.optString("from",null);
				return true;
			} catch (JSONException e) {
				RobotDebug.d(TAG, "语音控制格式指令解析 设防撤防  not json format. e : " +e.toString());
			}
		}
		
		/**
		 * 语音控制格式指令解析
		 */
		if(!data.isNull("from"))
		{
			try {
				fromvoice = data.getString("from");
				env = data.getBoolean("env");
				
				if(!env)
				{
					device_name = data.getString("device");
					operation = data.getString("operation");
					place = data.getString("place");
					quantity = data.optInt("quantity", -1);
				}	

				return true;
			}
			catch (JSONException e) {
					RobotDebug.d(TAG, "语音控制格式指令解析  not json format. e : " +e.toString());
			}
		}
		/**
		 * APP控制指令格式解析
		 */
		try {
			opcode = data.getInt("opcode");	
		} catch (JSONException e) {
			RobotDebug.d(TAG, "in smartDevice json String. not find opcode.");
			return false;
		}
		if(opcode == 0)
			return false;
		switch (opcode)
		{
      		case TransferCode.smartDev_Add_Del_Update:
      			try {
      				opt = data.getString("opt");
      				count = data.getInt("count");
      				device_array = data.getJSONArray("devices");
      			} catch (JSONException e) {
      				RobotDebug.d(TAG, "in zigbeeDev_Add_Del_Update : " +e.toString());
      			}
      			break;
      		case TransferCode.smartDev_Control:
      			try{
      				opt = data.getString("opt");
      				species = data.getString("species");
      				device_array  = data.getJSONArray("devices");
      			}catch(JSONException e){
      				RobotDebug.d(TAG, "in zigbeeDev_Control : " +e.toString());
      			}
      			break;
      		case TransferCode.smartDev_History_Data:
      		{
      			try{
      				index = data.getInt("index");
      				species = data.getString("species");
      				ieeeaddr  = data.getString("ieeeaddr");
      			}catch(JSONException e){
      				RobotDebug.d(TAG, "in zigbeeDev_Control : " +e.toString());
      			}
      		}
      			break;
      		case TransferCode.smartDev_Inquire:
      			try{
      				mode =  data.getString("mode");
      				if(! data.isNull("species"))
      					species = data.getString("species");
      				if(! data.isNull("ieeeaddr"))
      					ieeeaddr = data.getString("ieeeaddr");
      				if(! data.isNull("endpointid"))
      					endpointid = data.getInt("endpointid");
      				if(! data.isNull("name"))
      					devname = data.getString("name");
      				if(!data.isNull("addr"))
      					addr = data.getInt("addr");
      			}catch(JSONException e){
      				RobotDebug.d(TAG, "in zigbeeDev_Control : " +e.toString());
      			}
      			break;
      		case TransferCode.smartDev_Recent_Devlist:
      			{
      				try {
      					opt = data.getString("opt");
      				} catch (JSONException e) {
      					RobotDebug.d(TAG, "in smartDev_Recent_Devlist : " +e.toString());
      				}
      			}
      			break;
      		case TransferCode.smartDev_Security:
      			{
      				try {
      					opt = data.getString("opt");
      				} catch (JSONException e) {
      					RobotDebug.d(TAG, "in smartDev_Security : " +e.toString());
      				}
      			}
      		default:
      			RobotDebug.d(TAG, "opcode can be recognized.");
      			break;
		}
		return true;
	}
	
	@Override
	public void doing() {
		RobotDebug.d(TAG, "in smart device doing. opt： " + opt);
		ZigbeeService zgbService = (ZigbeeService) Robot.getInstance().getService(ZigbeeService.NAME);
		/**
		 * 语音指令处理--智能外设的操作
		 */
		if(fromvoice != null)
		{
			if(!zgbService.optDeviceForChat(operation, device_name, place, env)){
				// pass to infrared to handle
				InfraredService infraredService = (InfraredService) Robot.getInstance().getService(InfraredService.NAME);
				infraredService.handleVoiceCmd(operation, device_name, place, quantity);
			}
			return;
		}
		/**
		 * 语音指令处理----撤防设防433外设的设置
		 */
		Dev433Service dev433Service = (Dev433Service) Robot.getInstance().getService(Dev433Service.NAME);
		if(sercurity != null)
		{
			if(sercurity.equals(new String("on")))
				dev433Service.setSercurityOn(true);
			else if (sercurity.equals(new String("off")))
				dev433Service.setSercurityOn(false);
			
			try {
				DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
				if(ser_formvoice != null) {
					// 来自语言
					service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
							.put("function_code", "safe_setting").put("function_status", sercurity.equals("on")?"start":"end")
							.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
							.put("phone_no", XmppService.phone_Number)
							.put("control_way",ser_formvoice).toString());
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return;
		}
		
		/**
		 * APP控制指令的处理
		 */
		XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
		JSONObject msgbody = new JSONObject();
		switch (opcode)
		{
      		case TransferCode.smartDev_Add_Del_Update:
      			 {
   					RobotDebug.d(TAG, "smartDev_Add_Update count: " + count);   					
   					
      				try {
   						JSONArray devices_result = new JSONArray();
      					String result = null;
      					
      					if(ZigbeeService.areAllZigbeeDevices(device_array)){ // zigbee devices
          					if((opt.equals("add")) && 
          							(!zgbService.isNameDiffFromEachOther(device_array) || 
          									zgbService.isAnyNameInWhiteDevice(device_array) || 
          									isAnyNameExistedInRemoteController(device_array)) )
          					{ // make sure the names are different from each other, and not existed.
          						RobotDebug.d(TAG, " in add.....1: count : " + count);
          						for(int i = 0; i < device_array.length(); i++)
    	      					{
          							JSONObject json_ret = new JSONObject();
          							json_ret.put("ret", 1);
          							json_ret.put("device", device_array.getJSONObject(i));
          							json_ret.put("error_msg", "设备名称已存在");
          							devices_result.put(json_ret);
    	      					}
          					} else if(opt.equals("del") && isSwitch(device_array)) {
          							RobotDebug.d(TAG, " in del.....1: count : " + count);
          							result = zgbService.delDevice_forswitch(device_array.getJSONObject(0).toString());
          							devices_result = new JSONArray(result);
          							for(int i = 0; i < devices_result.length(); i++)
        	      					{
          								JSONObject json_ret = devices_result.getJSONObject(i);
        	      					}
          					} else if(opt.equals("update")){
          						if(isAnyNameExistedInRemoteController(device_array)){          							
          							for(int i = 0; i < device_array.length(); i++) {
              							JSONObject json_ret = new JSONObject();
              							json_ret.put("ret", 1);
              							json_ret.put("device", device_array.getJSONObject(i));
              							json_ret.put("error_msg", "设备名称已存在");
              							devices_result.put(json_ret);
        	      					}
          						}else{
              						result = zgbService.updateDeviceAll(device_array);
              						devices_result = new JSONArray(result);
          						}
          					} else {
    		      					for(int i = 0; i < device_array.length(); i++)
    		      					{
    		      						JSONObject devDetail = device_array.getJSONObject(i);
    		      						if(!devDetail.isNull("species"))
    		      						{
    		      							result = zgbService.optDevice(devDetail.toString(), opt, devDetail.getString("species"));
    		      							
    		      							RobotDebug.d(TAG, "smartDev_Add_Del_Update result :  " + result);
    		      							if(result != null)
    		      							{
    		      								JSONObject result_json = new JSONObject(result);
    		      								RobotDebug.d(TAG, "smartDev_Add_Del_Update result_json :  " + result_json.toString());
    		      								devices_result.put(result_json);
    		      							}
    		      						}
    		      					}
          					}
       					}else{
       						for(int i = 0; i < device_array.length(); i++)
          					{
          						JSONObject devDetail = device_array.getJSONObject(i);
          						if(!devDetail.isNull("species"))
          						{
          							if(isDev433(devDetail.getString("species")))
          							{
          								RobotDebug.d(TAG, "433 species: "+ devDetail.getString("species"));
          								result = dev433Service.optDevice(devDetail.toString(), opt, null);
          							} 
          							
          							RobotDebug.d(TAG, "smartDev_Add_Del_Update result :  " + result);
          							if(result != null)
          							{
          								JSONObject result_json = new JSONObject(result);
          								RobotDebug.d(TAG, "smartDev_Add_Del_Update result_json :  " + result_json.toString());
          								devices_result.put(result_json);
          							}
          						}
          					}
       					}
      					
      					msgbody.put("opcode",Integer.toString(opcode));
      					msgbody.put("opt", opt);
      					msgbody.put("result", 0);
      					msgbody.put("devices_result", devices_result);
          				xmppservice.sendMsg("R2C", msgbody.toString(), opcode);	
         			 } catch (JSONException e) {
         				 e.printStackTrace();
         			 }      				
      			 }
      			 break;
      		case TransferCode.smartDev_Control:
      			 {
      				RobotDebug.d(TAG, " in smartDev_Control.....1 ");
      				JSONArray devices_result = new JSONArray();
  					boolean  hasoptfailed = false;
  					String result = null;
      				try {
      					for(int i = 0; i < device_array.length(); i++)
      					{
      						JSONObject devDetail = device_array.getJSONObject(i);
      						result = zgbService.optDevice(devDetail.toString(), opt, null);
      						RobotDebug.d(TAG, " in smartDev_Control.....result: " +  result);
      						if(result != null)
      						{
      							JSONObject result_json = new JSONObject(result);
      							if(result_json.getInt("ret") != 0)
      								hasoptfailed = true;
      							devices_result.put(result_json);
      						}
      					}
      					msgbody.put("opcode",new String("" +opcode));
      					msgbody.put("opt", opt);
      					msgbody.put("species", species);
//      					if(hasoptfailed)
//      						msgbody.put("result", 1);
//      					else
      						msgbody.put("result", 0);
      					msgbody.put("devices_result", devices_result);
          				xmppservice.sendMsg("R2C", msgbody.toString(), opcode);	
      				} catch (JSONException e) {
      					e.printStackTrace();
      				}
      			 }
      			 break;
      		case TransferCode.smartDev_History_Data:
      			 {
      				RobotDebug.d(TAG, " in smartDev_History_Data.....1 ");
      				JSONObject result = zgbService.getDeviceHistoryData(species,index,ieeeaddr);
      				if(result != null)
      					xmppservice.sendMsg("R2C", result.toString(), opcode);
      				RobotDebug.d(TAG, " in smartDev_History_Data.....2 ");
      			 }
      			 break;
      		case TransferCode.smartDev_Inquire:
      			RobotDebug.d(TAG, "Inquire mode: "+ mode);
      			if(mode.equals(inquire_mode[0]))
      			{
      				JSONArray zgbtemp = zgbService.getAllDevArray();
      				JSONArray dev433temp = dev433Service.getAllDevArray();
      				int device_size = zgbtemp.length() + dev433temp.length();
      				for(int i = 0; i<dev433temp.length();i++)
      				{
      					try {
							zgbtemp.put(dev433temp.get(i));
						} catch (JSONException e) {
							e.printStackTrace();
						}
      				}
      				try {
						msgbody.put("opcode",new String("" +opcode));
						msgbody.put("mode", mode);
	      				msgbody.put("species", species);
	      				msgbody.put("count", device_size);
	      				msgbody.put("devices", zgbtemp);
	      				xmppservice.sendMsg("R2C", msgbody.toString(), opcode);
					} catch (JSONException e) {
						e.printStackTrace();
					}	
      			}	
      			else
      			{
      				JSONArray result = null;
      				if(mode.equals(inquire_mode[1]))
      				{
      					if(isDev433(species))
      					{
      						RobotDebug.d(TAG, "433 Inquire mode 1 type :  "+ mode);
      						result = dev433Service.inquireDevice(mode,species,addr,devname);
      					}
      					else
      					{
      						RobotDebug.d(TAG, "zgb Inquire mode 1 type :  "+ mode);
      						result = zgbService.inquireDevice(mode, species, ieeeaddr, endpointid, devname);
      					}
      				}
      				else if (mode.equals(inquire_mode[2]))
      				{
      					if(addr == 0)
      						result =  zgbService.inquireDevice(mode, species, ieeeaddr, endpointid, devname);
          				else
          					result =  dev433Service.inquireDevice(mode,species, addr,devname);
      				}
      				else
      					RobotDebug.d(TAG, "in smartDevice action mode :" + mode + " not support.");
      				RobotDebug.d(TAG,"result : " + result.toString());
      				if(result != null)
      				{
      					try {
    						msgbody.put("opcode",new String("" +opcode));
    						msgbody.put("mode",mode);
    	      				msgbody.put("species",species);
    	      				msgbody.put("count",result.length());
    	      				msgbody.put("devices",result);
              				xmppservice.sendMsg("R2C", msgbody.toString(), opcode);
    					} catch (JSONException e) {
    						RobotDebug.d(TAG, "in get smartDev_Inquire devices not json format. e: " +e.toString());
    					}
      				}
      			}
      			break;
      		case TransferCode.smartDev_Recent_Devlist:
      			 {
      			    try {
						JSONArray temp_devices = new JSONArray(zgbService.getRecentDevices());
						msgbody.put("opcode",new String("" +opcode));
						msgbody.put("count", temp_devices.length());
						msgbody.put("devices", temp_devices);
						xmppservice.sendMsg("R2C", msgbody.toString(), opcode);
					} catch (JSONException e) {
						RobotDebug.d(TAG, "in get SmartDev_Recent_DevList devices not json format. e: " +e.toString());
					}
      			 }
      			 break;
      		case TransferCode.smartDev_Security:
      			try
      			{
      				boolean ret = false;
      				if(opt.equals(new String("use")))
      					ret = dev433Service.setSercurityOn(true);
      				else
      					ret = dev433Service.setSercurityOn(false);
      				
      				msgbody.put("opcode", new String("" + opcode));
      				msgbody.put("opt", opt);
//      				if(ret == false)
//      					msgbody.put("result", 1);
//      				else
      					msgbody.put("result", 0);
      				xmppservice.sendMsg("R2C", msgbody.toString(), opcode);
      				
      			// 来自app
      			DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
				service.sendDataToXmppService(new JSONObject().put("todo","report_r_func_status").put("curr_version", RobotVersion.current_system_sw_version)
							.put("function_code", "safe_setting").put("function_status", opt.equals("use")?"start":"end")
							.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
							.put("phone_no", XmppService.phone_Number)
							.put("control_way","app_button").toString());	
      			} catch (JSONException e) {
					RobotDebug.d(TAG, "in get smartDev_Security  not json format. e: " +e.toString());
				}
      		default:
      			RobotDebug.d(TAG, "opcode can be recognized.");
      			break;
		}
		RobotDebug.d(TAG, "in smart device doing.");
	}
	
	public boolean isArrayListDeviceSame(JSONArray devices_list)
	{
		
		if(devices_list.length() < 2)
			return false;
		try {
			if(devices_list.getJSONObject(0).isNull("Ieeeaddr"))
				return false;
		} catch (JSONException e1) {
			RobotDebug.d(TAG, "in isArrayListDeviceSame no Ieeeaddr is not zigbee device.");
			return false;
		}
		boolean isAllSame = true;
		for(int i = 0 ; i < devices_list.length()-1;i++)
		{
			try {
				JSONObject temp = devices_list.getJSONObject(i);
				JSONObject temp2 = devices_list.getJSONObject(i+1);
				if(! temp.getString("Ieeaddr").equals(temp2.getString("Ieeaddr")))
				{
					isAllSame = false;
					break;
				}
			} catch (JSONException e) {
				RobotDebug.d(TAG, "isArrayListDeviceSame is not json fomart : " + e.toString());
			}
			if(!isAllSame)
			{
				return false;
			}
		}
		return true;
	}
	/**
	 * 判断是否是开关类设备
	 * @param devices_list
	 * @return
	 */
	public boolean isSwitch(JSONArray devices_list)
	{
		if(devices_list.length() <= 0)
			return false;
		try {
			if(devices_list.getJSONObject(0).isNull("ieeeaddr"))
				return false;
			if(devices_list.getJSONObject(0).getString("species").equals(new String("switch")))
				return true;
			else
				return false;
		} catch (JSONException e1) {
			RobotDebug.d(TAG, "in isArrayListDeviceSame no Ieeeaddr is not zigbee device.");
			return false;
		}
	}	
	
	private boolean isAnyNameExistedInRemoteController(JSONArray devices) throws JSONException{
		InfraredService infraredService = (InfraredService) Robot.getInstance().getService(InfraredService.NAME);
		
		for (int i = 0; i < devices.length(); i++) {
			String name = devices.getJSONObject(i).getString("name");
			String location = devices.getJSONObject(i).getString("location");
							
			if(infraredService.isRemoteControllerExisted(location, name)){
				return true;
			}
		}

		return false;
	}
	
}
