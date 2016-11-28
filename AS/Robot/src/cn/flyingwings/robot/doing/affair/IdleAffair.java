package cn.flyingwings.robot.doing.affair;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.FaceService.FaceService;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.AudioAmplifiterService;
import cn.flyingwings.robot.service.LCDBackLightService;
import cn.flyingwings.robot.service.RobotStatusInfoService;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

/**
 * idle 事务是一个特殊的事务，任何时候当没有其他事务运行时，任务调度器会尝试启动 idle 事务。启动任务为 一个没有意义的 {@link RobotAction} 实例，忽略即可。
 * 进入idle待机事务后，1分钟以后进行电量检测，如果低于20%，进行找充电座操作。否则继续待机7分钟，并且关闭屏幕和Audio功放。
 * 另外，进入待机后，每隔30分钟进行一次电量检测，低于20%，进行找充电座
 * @author gd.zhang
 *
 */
public class IdleAffair extends RobotAffair {
	
	public static final String NAME = "idle";
	private long countTimer = 0;
	public ScheduledThreadPoolExecutor scheduledThread  = null;
	
	private TimerTask task = new TimerTask() {
		
		@Override
		public void run() {
			RobotDebug.d(NAME, "检测idle。。");
			countTimer++;
			/**
			 * 1分钟开始检测电量,低于20%,并且不在充电座上，进行找充电座操作
			 */
			if( countTimer == (60*1) ) {
				RobotDebug.d(NAME, "1分钟检测电量。。");
				RobotStatusInfoService infoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
				if(infoService != null) {
					int level = infoService.getBatteryLevel();
					int status = infoService.getBatteryStatus();
					if( (status == 0) && (level < 20) && (level > 10) ) {
						try {
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "seek_home").put("charge", 1).put("isAuto", true).put("from", "low_battery").toString());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				return;
			}
			/**
			 * 8分钟后，开始执行关闭屏幕和Audio功放
			 */
			if(countTimer == (60*8)) {
				// 硬件存在lcd不能把背光电压下拉的问题，导致修改成显示黑图片
//				LCDBackLightService lcdbacklight = (LCDBackLightService) Robot.getInstance().getService(LCDBackLightService.NAME);
//				lcdbacklight.lcdOff();
				AssetManager asmanager = Robot.getInstance().getContext().getAssets();
				InputStream bitmapInputStream;
				try {
					bitmapInputStream = asmanager.open("imgRes/idle_black.jpg");
					Bitmap bitmap = BitmapFactory.decodeStream(bitmapInputStream);
					FaceService face_ser = (FaceService) Robot.getInstance().getService(FaceService.NAME);
					face_ser.showBitMap(bitmap);
				} catch (IOException e) {
					e.printStackTrace();
				}
				AudioAmplifiterService audioservice = (AudioAmplifiterService) Robot.getInstance().getService(AudioAmplifiterService.NAME);
				audioservice.audioOff();
				return;
			}
			
			/**
			 * 电量不足10%，需要充电，请求帮助
			 */
			if(countTimer %(60*10) == 0) {
				RobotDebug.d(NAME, "10分钟检测电量。。");
				RobotStatusInfoService infoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
				if(infoService != null) {
					int level = infoService.getBatteryLevel();
					int status = infoService.getBatteryStatus();
					if( (status == 0) && (level < 10) && (level > 3) ) {
						try {
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "say").put("content", "电量不足10%，需要充电，请求帮助").toString());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			

			/**
			 * 每隔30分钟检测一次电量
			 */
			if((countTimer%(60*30)) == 0) {
				RobotStatusInfoService infoService = (RobotStatusInfoService) Robot.getInstance().getService(RobotStatusInfoService.NAME);
				if(infoService != null) {
					int level = infoService.getBatteryLevel();
					int status = infoService.getBatteryStatus();
					if( (status == 0) && (level < 20)  && (level > 10) ) {
						try {
							Robot.getInstance().getManager().toDo(new JSONObject().put("name", "seek_home").put("charge", 1).put("isAuto", true).put("from", "low_battery").toString());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				return;
			}
		}
	};
	
	@Override
	public String name() {
		return NAME;
	};

	public IdleAffair(RobotAffairManager am) {
		super(am);
	}
	
	
	@Override
	protected void onAction(RobotAction act) {
		super.onAction(act);
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		RobotDebug.d(NAME,"启动 idle 事务");
		super.onCreated(act);
		scheduledThread = new ScheduledThreadPoolExecutor(1);
		scheduledThread.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
	}

	@Override
	protected void onFinished() {
		super.onFinished();
		RobotDebug.d(NAME,"结束 idle 事务");
		if(scheduledThread != null) {
			scheduledThread.shutdownNow();
			scheduledThread = null;
		}
		LCDBackLightService lcdbacklight = (LCDBackLightService) Robot.getInstance().getService(LCDBackLightService.NAME);
		lcdbacklight.lcdOn();
		AudioAmplifiterService audioservice = (AudioAmplifiterService) Robot.getInstance().getService(AudioAmplifiterService.NAME);
		audioservice.audioOn();
		FaceService face_ser = (FaceService) Robot.getInstance().getService(FaceService.NAME);
		face_ser.dismissDialog();
	}
}
