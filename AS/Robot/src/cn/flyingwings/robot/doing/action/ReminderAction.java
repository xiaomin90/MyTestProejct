package cn.flyingwings.robot.doing.action;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.service.ScheduleService;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

/**
 * 增删改:{"name":"reminder","opcode":xxx,"opt":"","event":{"xxxxx"}}
 * 查询: {"name":"reminder","opcode":xxx,"index":xx};
 * @author min.js
 *
 */
public class ReminderAction extends RobotAction {

	public  static String NAME = "reminder";
	private int     opcode = -1;
	private String  opt  = null;
	private int     tid  = -1;
	private String  time = null;
	private int     week = -1;
	private String  content = null;
	public  JSONObject  event   = null;
	public  boolean enable  = false;
	public  int index = -1;
	
	
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
			JSONObject datas = new JSONObject(s);
			opcode = datas.getInt("opcode");
			RobotDebug.d(NAME,"opcode :  " + opcode);
			if(opcode == TransferCode.robot_reminder_add_del_update)
			{
				opt = datas.getString("opt");
				event = datas.getJSONObject("event");
				if(event != null)
				{
					time = event.getString("time");
					week = event.getInt("week");
					content = event.getString("content");
					if(event.getString("enable").equals(new String("T")))
						enable = true;
					else 
						enable = false;
					if(!event.isNull("id"))
						tid = event.getInt("id");
				}
			}
			else if(opcode == TransferCode.robot_reminder_inquiry)
			{
				index = datas.getInt("index");
			}
			else
				RobotDebug.d(NAME, "opcode unknown :  "+ opcode);
			
		} catch (JSONException e) {
			RobotDebug.d(NAME, "parse reminder json format e: " + e.toString());
		}
		return true;
	}
	
	@SuppressWarnings("null")
	public void doing() {
		if(opcode == -1)
			return;
		ScheduleService scheduleService = (ScheduleService) Robot.getInstance().getService(ScheduleService.NAME);
		if(scheduleService == null)
			return;
		switch(opcode)
		{
			case  TransferCode.robot_reminder_add_del_update:
				{
					/**
					 * 添加 删除 修改
					 */
					if(opt == null)
					{
						RobotDebug.d(NAME, " opt is null");
						return;
					}
						
					if(opt.equals(new String("add")))
					{
						try {
							String error_msg = "添加失败";
							JSONObject ret_json = new JSONObject();
							int tid = scheduleService.addTaskForApp(content, new JSONObject().put("name", "say").put("content", content).toString(), time, week);
							if(tid == -1)
							{
								ret_json.put("result",1);
								ret_json.put("error_msg",error_msg);
							}
							else
							{
								scheduleService.setEnable(tid,true);
								ret_json.put("result",0);
								event.put("id", tid);
							}
							ret_json.put("opt", opt);
							ret_json.put("opcode", new String("" + TransferCode.robot_reminder_add_del_update));
							ret_json.put("event", event);
							XmppService xmppservice = (XmppService)Robot.getInstance().getService(XmppService.NAME);
							xmppservice.sendMsg("R2C", ret_json.toString(),TransferCode.robot_reminder_add_del_update);
						} catch (JSONException e) {
							RobotDebug.d(NAME," add reminder e: " + e.toString());
						}
						
					}
					else if(opt.equals(new String("update")))
					{
						try {
							String error_msg = "更新失败 tid不存在";
							JSONObject ret_json = new JSONObject();
							boolean ret_modify = scheduleService.modifyTask(tid, content, new JSONObject().put("name", "say").put("content", content).toString(), time, week);
							if(!ret_modify)
							{
								ret_json.put("result",1);
								ret_json.put("error_msg",error_msg);
							}
							else
								ret_json.put("result",0);
							ret_json.put("opt", opt);
							ret_json.put("opcode", new String("" + TransferCode.robot_reminder_add_del_update));
							ret_json.put("event", event);
							XmppService xmppservice = (XmppService)Robot.getInstance().getService(XmppService.NAME);
							xmppservice.sendMsg("R2C", ret_json.toString(),TransferCode.robot_reminder_add_del_update);
						} catch (JSONException e) {
							RobotDebug.d(NAME," add reminder e: " + e.toString());
						}
					}
					else if(opt.equals(new String("enable")))
					{
						try {
							String error_msg = "更新失败 id不存在";
							JSONObject ret_json = new JSONObject();
							if(tid == -1)
							{
								RobotDebug.d(NAME,"tid -1  不能enable");
							}
							boolean ret_modify = scheduleService.setEnable(tid,enable);
							if(!ret_modify)
							{
								ret_json.put("result",1);
								ret_json.put("error_msg",error_msg);
							}
							else
								ret_json.put("result",0);
							ret_json.put("opt", opt);
							ret_json.put("opcode", new String("" + TransferCode.robot_reminder_add_del_update));
							ret_json.put("event", event);
							XmppService xmppservice = (XmppService)Robot.getInstance().getService(XmppService.NAME);
							xmppservice.sendMsg("R2C", ret_json.toString(),TransferCode.robot_reminder_add_del_update);
						} catch (JSONException e) {
							RobotDebug.d(NAME," add reminder e: " + e.toString());
						}
					}
					else if(opt.equals(new String("delete")))
					{
						try {
							String error_msg = "删除失败tid不存在";
							JSONObject ret_json = new JSONObject();
							boolean ret_delete = scheduleService.delTask(tid);
							if(!ret_delete)
							{
								ret_json.put("result",1);
								ret_json.put("error_msg",error_msg);
							}
							else
								ret_json.put("result",0);
							ret_json.put("opt", opt);
							ret_json.put("opcode",new String("" + TransferCode.robot_reminder_add_del_update));
							ret_json.put("event",event);
							XmppService xmppservice = (XmppService)Robot.getInstance().getService(XmppService.NAME);
							xmppservice.sendMsg("R2C", ret_json.toString(),TransferCode.robot_reminder_add_del_update);
						} catch (JSONException e) {
							RobotDebug.d(NAME," add reminder e: " + e.toString());
						}
					}
					else
					{
						RobotDebug.d(NAME, "unkown opt : " + opt);
					}
					
				}
				break;
			case  TransferCode.robot_reminder_inquiry:
				{
					try {
						JSONObject ret_json = new JSONObject();
						JSONArray inquiry_result = scheduleService.queryAllTask(index);
						ret_json.put("result",0);
						ret_json.put("opcode", new String("" + TransferCode.robot_reminder_inquiry));
						ret_json.put("index", index);
						ret_json.put("events", inquiry_result);
						XmppService xmppservice = (XmppService)Robot.getInstance().getService(XmppService.NAME);
						xmppservice.sendMsg("R2C", ret_json.toString(),TransferCode.robot_reminder_inquiry);
						RobotDebug.d(NAME, " 3333");
					} catch (JSONException e) {
						RobotDebug.d(NAME," inquiry reminder e: " + e.toString());
					}
				}
				break;
			default:
				RobotDebug.d(NAME, "unkown opcode : " + opcode);
				break;
		}
		
		
		
	}
	
	
	
}
