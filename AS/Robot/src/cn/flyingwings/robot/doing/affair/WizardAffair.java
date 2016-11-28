package cn.flyingwings.robot.doing.affair;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.util.Base64;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.FaceService.FaceService;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.BarCodeService;
import cn.flyingwings.robot.xmppservice.XmppService;

/**
 * 开机向导事务
 * @author min.js
 */
public class WizardAffair extends RobotAffair {

	public static String NAME = "wizard";
	public static String BoardCastIntent = "cn.flyingwings.robot.FinishWizardAffair";
	private BroadcastReceiver receiver = null;
	public WizardAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	protected void onCreated(RobotAction act) {
		RobotDebug.d(NAME,"wizard Affair Start ");
		/** 
		 * 等待播报音乐完成
		 */
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//显示自己的机器人号的二维码
		FaceService face_ser = (FaceService) Robot.getInstance().getService(FaceService.NAME);
		try {
			JSONObject json_obj = new JSONObject();
			json_obj.put("sn", XmppService.robot_SN);
			json_obj.put("key", XmppService.password);
			RobotDebug.d(NAME, "二维码信息 json format : " + json_obj.toString());
			byte[] temp = Base64.encode(json_obj.toString().getBytes(),Base64.DEFAULT);
			Bitmap bitmap = BarCodeService.createQRImage(new String(temp));
			face_ser.showQrCode(bitmap, -1); // 5分钟 
		} catch (JSONException e) {
			RobotDebug.d(NAME, "二维码信息 json format e: " + e.toString());
		}
		
		receiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				  RobotDebug.d(NAME,"apprtcAffair receive : " + intent.getAction());
				  boolean isActivate = intent.getBooleanExtra("activated",false);
				  if(isActivate)
				  {
					  RobotDebug.d(NAME, "robot activate true.");
					  try {
							JSONObject json_obj = new JSONObject();
							json_obj.put("name", "say");
							json_obj.put("content", "您就是我的管理员了！有什么任务您尽管安排吧，维拉会努力完成的！");
							Robot.getInstance().getManager().toDo(json_obj.toString());
						  } catch (JSONException e) {
							RobotDebug.d(NAME, "二维码信息 json format e: " + e.toString());
						  }
					  try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 stopAffair(true);
				  }
			}
		};
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BoardCastIntent); 
		Robot.getInstance().getContext().registerReceiver(receiver,intentFilter); 
	}
	
	@Override
	protected synchronized void onFinished() {
		RobotDebug.d(NAME, "wizard Affair is finished.");
		if(receiver != null){
			Robot.getInstance().getContext().unregisterReceiver(receiver);
			receiver = null;
		}
		FaceService face_ser = (FaceService) Robot.getInstance().getService(FaceService.NAME);
		face_ser.dismissDialog();
	}
	

}
