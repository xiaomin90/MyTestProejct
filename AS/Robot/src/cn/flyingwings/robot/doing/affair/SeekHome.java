package cn.flyingwings.robot.doing.affair;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.RobotVersion;
import cn.flyingwings.robot.datastatistics.DataStatisticsService;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.CANService;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import cn.flyingwings.robot.service.CANService.CANCallback;
import cn.flyingwings.robot.service.CANService.CANMessage;
import cn.flyingwings.robot.xmppservice.XmppService;

public class SeekHome extends RobotAffair {	
	public static final String NAME = "seek_home";
	private String TAGS = "SeekHome";
	
	private static final String  cancelSeekHomeAction = "cn.flyingwings.robot.SeekHome.Cancel";	
	
	private SeekHomeStatusCb seekHomeStatusCb = null;
	private BroadcastReceiver receiver;
	private boolean brregistered = false;

	private enum Status{
		UNKNOWN, SUCCESS, FAIL, CANCEL
	}
	private Status status = Status.UNKNOWN;
	
	public SeekHome(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return SeekHome.NAME;
	}	
	
	protected void onCreated(RobotAction act) {
		Log.d(TAGS,"seekhome Affair Start ");
		seekHomeStatusCb = new SeekHomeStatusCb();
		CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
		canservice.addCallback(0xE0, seekHomeStatusCb);
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				  Log.d(TAGS,"SeekHome Affair  receive : " + cancelSeekHomeAction);
				  String action = intent.getAction();
	              if( action.equals(cancelSeekHomeAction)) {
	            	   status = Status.CANCEL;
	            	   stopAffair(true); 
	              }
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(cancelSeekHomeAction); 
		Robot.getInstance().getContext().registerReceiver(receiver,intentFilter); 
		brregistered = true;
	}
	
	protected synchronized void onFinished() {
		Log.d(TAGS, "seekhome Affair is finished." + " status: " + status);
		if(brregistered) {
			Robot.getInstance().getContext().unregisterReceiver(receiver);
			brregistered = false;
			/**
			 *  找不到需要播放该提示音
			 *  电量大于等于10%，维拉找不到充电桩，请求帮助
			 *  电量小于10%,维拉电量不足10%，不久后将自动关机，请求帮助
			 */
			if(status == Status.FAIL) {
				try {
					RobotStatusInfoService statusInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
					if(!statusInfoService.isDndMode()) {
						int level = statusInfoService.getBatteryLevel();
						if ( level > 20)
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", "维拉找不到充电桩,请求帮助").toString());
						else if ( (level >= 10) && (level <= 20))
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", "维拉电量不足20%,维拉找不到充电桩,请求帮助").toString());
						else if(level < 10)
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", "维拉电量不足10%,不久后将自动关机,请求帮助").toString());
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
		canservice.removeCallback(0xE0, seekHomeStatusCb);
		if(Status.UNKNOWN == status) { // interrupted by other affair
			reportResult(false);
			CANService.CANMessage canmsg = new CANService.CANMessage();
			canmsg.set(0xE0, 0x00, new byte[]{0x0});
			canservice.send(canmsg);
		}
		//---------------------------通知服务器----- start ---------------//
		String end_reason = null;
		if(status == Status.CANCEL) {
			end_reason = "canceled";
		} else if(status == Status.SUCCESS) {
			end_reason = "success";
		} else if(status == Status.FAIL) {
			end_reason = "failure";
		} else if(status == Status.UNKNOWN) {
			end_reason = "interrupted";
		}
		//---------------------------通知服务器----- start ---------------//
		try {
			DataStatisticsService service = (DataStatisticsService) Robot.getInstance().getService(DataStatisticsService.NAME);
			JSONObject json = new JSONObject().put("todo","report_r_func_status")
										.put("curr_version", RobotVersion.current_system_sw_version)
										.put("function_code", "find_charger")
										.put("function_status", "end")
										.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
										.put("phone_no", XmppService.phone_Number)
										.put("end_reason",end_reason);
		 	service.sendDataToXmppService(json.toString());
		//--------------------------------通知服务器------start----- ---//
		 	if(status == Status.SUCCESS) {
				RobotStatusInfoService infoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
				json = new JSONObject().put("todo","report_r_func_status")
											.put("curr_version", RobotVersion.current_system_sw_version)
											.put("function_code", "recharge")
											.put("function_status", "start")
											.put("occur_time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
											.put("phone_no", XmppService.phone_Number)
											.put("start_reason","find_charger")
											.put("curr_battery",infoService.getBatteryLevel());
			 	service.sendDataToXmppService(json.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}	
		//--------------------------通知服务器----- end----------------//
	}
	
	private void reportResult(boolean successful) {
		try {
			JSONObject rcmsg = new JSONObject();
			rcmsg.put("opcode", "93402")
					.put("charge", successful ? "success" : "fail");

			XmppService xmppservice = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			xmppservice.sendMsg("R2C", rcmsg.toString(), 93402);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private class SeekHomeStatusCb implements CANCallback {
		
		@Override
		public void onMessage(CANMessage msg) {
			if (msg.getId() == 0xE1) {
				CANService canservice = (CANService) Robot.getInstance().getService(CANService.NAME);
				canservice.removeCallback(0xE0,seekHomeStatusCb);
				byte result = msg.data[0];
				if (1 == result) { // found
					status = Status.SUCCESS;
					RobotStatusInfoService statusInfoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
					if(!statusInfoService.isDndMode()) {
						//维拉开始充电了
						try {
							Robot.getInstance().getManager().toDo(new JSONObject().put("name","say").put("content", "维拉开始充电了").toString());
						} catch (JSONException e) {
							e.printStackTrace();
						}
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else if (0 == result) { // not found
					status = Status.FAIL;
					//palyrecord("sound/battery_low0.mp3");
				} else { // err
					Log.e(TAGS, "not support status: " + result);
					stopAffair(true);
					return ;
				}
				reportResult(Status.SUCCESS == status);
				stopAffair(true);
			}
		}
	}

	/**
	 * 播放asset中的音乐
	 * @param path
	 */
	public void palyrecord(String path){
		MediaPlayer mediaplayer = null;
		try{
			mediaplayer = new MediaPlayer();
			Context  context = Robot.getInstance().getContext();
			AssetManager assMg = context.getAssets();
			AssetFileDescriptor fileDescriptor = null;
			fileDescriptor = assMg.openFd(path);
			mediaplayer.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						RobotDebug.d(NAME, "播放结束示音结束.");
					}
			});
			mediaplayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(),fileDescriptor.getLength());
			mediaplayer.prepare();
			fileDescriptor.close();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(mediaplayer == null)
			return ;
		mediaplayer.start();	
		try {
			Thread.sleep(mediaplayer.getDuration()+100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
