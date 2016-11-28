package cn.flyingwings.robot.doing.affair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.doing.action.ClientControl;
import cn.flyingwings.robot.doing.action.MusicPlayAction;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.MusicPlayService;

public class MusicAffair extends RobotAffair {
	

	public static final String NAME = "music_play";
	private boolean isRegist = false;
	public static final String action_finish_music = "cn.flyingwings.robot.finishMusic";
	public  boolean isUserStop  = false;

	public MusicAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}

	
	@Override
	protected void onAction(RobotAction act) {
		super.onAction(act);
		// 记录用户播放用来判断用户掉线时是否要关闭
		if(act instanceof MusicPlayAction) {
			if ( ((MusicPlayAction)act).opt.equals("stop") )
				isUserStop = true;
			else
				isUserStop = false;
			return;
		}
		
		if(act instanceof ClientControl) {
			if (((ClientControl)act).state.equals("disconnect")) {
				if ( isUserStop)  {
					Robot.getInstance().getContext().sendBroadcast(new Intent(action_finish_music));
				}
			}
			return;
		} 
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		IntentFilter intent = new IntentFilter(action_finish_music);
		Robot.getInstance().getContext().registerReceiver(receiver, intent);
		isRegist = true;
	}

	@Override
	protected void onFinished() {
		super.onFinished();
		MusicPlayService musicPlayService = (MusicPlayService) Robot.getInstance().getService(MusicPlayService.NAME);
		musicPlayService.stopMusicPlay();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public synchronized void onReceive(Context context, Intent intent) {
	        if(isRegist){
	        	isRegist = false;
	        	Robot.getInstance().getContext().unregisterReceiver(receiver);
	        	stopAffair(true);
	        }
		}
	};
}
