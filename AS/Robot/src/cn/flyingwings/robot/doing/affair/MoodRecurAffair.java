package cn.flyingwings.robot.doing.affair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.doing.action.MoodRecurAction;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.MusicPlayService;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

public class MoodRecurAffair extends RobotAffair{
	


	public static final String NAME = "mood_recur";
	public static final String FINISH_ACTION = "cn.flyingwings.robot.finishMoodRecur";
	private Context context; 
	private boolean hasStartAction = false;
	
	private boolean receiverRegistered = false;
	

	public MoodRecurAffair(RobotAffairManager am) {
		super(am);
		
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	protected synchronized void onCreated(RobotAction act) {
		super.onCreated(act);
		// 关闭音乐
		MusicPlayService service = (MusicPlayService)Robot.getInstance().getService(MusicPlayService.NAME);
		service.stopMusicPlay();
		context = Robot.getInstance().getContext();
		context.registerReceiver(receiver, new IntentFilter(FINISH_ACTION));
		receiverRegistered = true;
		RobotDebug.d(NAME, "register ..");
	}
	
	@Override
	protected void onAction(RobotAction act) {
		super.onAction(act);
		if(act instanceof MoodRecurAction){
			
			MoodRecurAction action = (MoodRecurAction)act;
			PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
			if(action.isVoice){
				hasStartAction = true;
				pathService.excuteMoodVoice(action.moodName);
				return ;
			}
			if (!TextUtils.isEmpty(action.opcode) && !TextUtils.isEmpty(action.opt)) {
				if(action.opt.equals("start")){
					
					if(action.mood != null){
						hasStartAction = true;
						pathService.excuteMood(action.mood);
					}
				}else if(action.opt.equals("cancel")){
					if(hasStartAction)
						pathService.cancelExcuteMood();
					else{
						XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
						RobotDebug.d(NAME, "stopAffair 11111111111");
						String cmd = "{\"opcode\":\"%s\", \"type\":\"mood\", \"result\":1, \"error_msg\":\"心情物语执行已经取消\"}";
						xmppService.sendMsg("R2C", String.format(cmd, String.valueOf(TransferCode.robot_path_excute_result)),
								TransferCode.robot_path_excute_result);
						pathService.cancelExcuteMood();
						//stopAffair(true);
					}
					//stopAffair(true);
				}
			}
		}
	}

	@Override
	protected synchronized void onFinished() {
		super.onFinished();
		RobotDebug.d(NAME, "unregister 1");
		if(receiverRegistered){
			RobotDebug.d(NAME, "unregister 2");
			context.unregisterReceiver(receiver);
			receiverRegistered = false;
		}
		hasStartAction = false;
		// 停止路径复现
		RobotDebug.d(NAME, "stopAffair 222222");
		PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		pathService.cancelExcuteMood();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			stopAffair(true);
		}
	};
	
}
