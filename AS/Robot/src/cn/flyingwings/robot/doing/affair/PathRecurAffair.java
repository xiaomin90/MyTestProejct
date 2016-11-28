package cn.flyingwings.robot.doing.affair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.PathService.PathService;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.SpeechObserver;
import cn.flyingwings.robot.chatService.SpeechObserver.SpeechObservable;
import cn.flyingwings.robot.doing.action.PathRecurAction;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.xmppservice.TransferCode;
import cn.flyingwings.robot.xmppservice.XmppService;

public class PathRecurAffair extends RobotAffair{
	public static final String NAME = "path_recur";
	public static final String FINISH_ACTION = "cn.flyingwings.robot.FinishPathRecur";
	private Context context; 
	private boolean receiverRegistered = false;
	private boolean hasStartAction = false;
	
	public PathRecurAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		context = Robot.getInstance().getContext();
		context.registerReceiver(receiver, new IntentFilter(FINISH_ACTION));
		receiverRegistered = true;
	}
	
	@Override
	protected void onAction(RobotAction act) {
		super.onAction(act);
		if(act instanceof PathRecurAction){
			PathRecurAction action = (PathRecurAction)act;
			PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
			if (action.opcode.equals("93103")){
				if(action.opt.equals("start")){
					
					hasStartAction = true;
					prePathRecur(action);
					
				}else if(action.opt.equals("cancel")){
					if(hasStartAction)
						pathService.cancelRecurPath();
					else{
						XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
						String cmd = "{\"opcode\":\"%s\", \"type\":\"route\", \"result\":1, \"error_msg\":\"路径执行已经取消\"}";
						xmppService.sendMsg("R2C", String.format(cmd, String.valueOf(TransferCode.robot_path_excute_result)),
								TransferCode.robot_path_excute_result);
						pathService.cancelRecurPath();
					}
				}
			}
		}
	}

	@Override
	protected synchronized void onFinished() {
		super.onFinished();
		if(receiverRegistered){
			context.unregisterReceiver(receiver);
			receiverRegistered = false;
		}
		hasStartAction = false;
		// 停止路径复现
		PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		pathService.cancelRecurPath();
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			stopAffair(true);
		}
	};
	
	private void prePathRecur(final PathRecurAction act){
		ChatService chatService = (ChatService) (Robot.getInstance().getService(ChatService.NAME));
		String content = "维拉开始" + act.pathName;
		chatService.ttsControll(content);
		try {
			long sleeptime = (long) (content.length()*1000*0.4);
			RobotDebug.d(NAME, "sleeptime : " + sleeptime);
			Thread.sleep(sleeptime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		PathService pathService = (PathService) Robot.getInstance().getService(PathService.NAME);
		RobotDebug.d(NAME, "sleeptime over");
		if(act.isVoice){
				pathService.excutePathVoice(act.pathId);
		}else{
				pathService.recurPath(act.pathId);
		}
		
	}
}
