package cn.flyingwings.robot.doing.affair;

import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.chatService.ChatDebug;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.ChatService.ControllEnum;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.chatService.SpeechRecognize;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.service.WakeupService;
import cn.flyingwings.robot.chatService.ModuleController.State;

public class ChatAffair extends RobotAffair {

	public static final String NAME = "chat";
	private final String TAG = "ChatAffair";

	public ChatAffair(RobotAffairManager am) {
		super(am);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	protected void onCreated(RobotAction act) {
		Log.e("voice_module", "ChatAffair onCreate");
		// TODO Auto-generated method stub
		ChatService mChatService = (ChatService) (Robot.getInstance().getService(ChatService.NAME));
		// mChatService.initAsr();
		mChatService.initTTs();
		// mChatService.asrControll(ControllEnum.start);
		super.onCreated(act);
	}

	@Override
	protected void onFinished() {
		// TODO Auto-generated method stub
		Log.e(TAG, " ChatAffair on finished.");
		// mChatService.asrControll(ControllEnum.close);
		ChatService mChatService = (ChatService) (Robot.getInstance().getService(ChatService.NAME));
		mChatService.ttsStop();
		SpeechRecognize.cancel();
		
		SpeechRecognize.setRecogIsClose(true);
		ChatDebug.i("RECOGNIZ_STATUS", "call onFinished() SpeechRecognize.RECOGNIZ_STATUS = SpeechRecognize.CLOSE");

		/*WakeupService wakeupService = (WakeupService) Robot.getInstance().getService(WakeupService.NAME);
		wakeupService.startWakeup();*/
		ModuleController.getInstance().changeState(ModuleController.State.WAKEUP, null);
		super.onFinished();
	}
}
