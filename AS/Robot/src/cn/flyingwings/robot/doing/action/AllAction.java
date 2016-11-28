package cn.flyingwings.robot.doing.action;

/**
 * 动作注册辅助类<br>
 * 将所有动作都通过这个文件统一注册，方便管理。
 * @author gd.zhang
 *
 */
public class AllAction {

	public static void initAllAction(RobotDivide rd) {
		rd.addAction(SetWifi.NAME, SetWifi.class);
		rd.addAction(ClientControl.NAME, ClientControl.class);
		rd.addAction(CallView.NAME, CallView.class);
		rd.addAction(Call.NAME, Call.class);
		rd.addAction(View.NAME, View.class);
		rd.addAction(ClientMove.NAME, ClientMove.class);
		rd.addAction(WakeUpAction.NAME, WakeUpAction.class);
		rd.addAction(SayAction.NAME, SayAction.class);
		rd.addAction(SeekHome.NAME,SeekHome.class);
		rd.addAction(Charge.NAME, Charge.class);
		rd.addAction(SmartDevice.NAME, SmartDevice.class);
		rd.addAction(Voice_app.NAME, Voice_app.class);
		rd.addAction(MusicPlayAction.NAME, MusicPlayAction.class);
		rd.addAction(UpdateAction.NAME, UpdateAction.class);
		rd.addAction(RobotInfoAction.NAME, RobotInfoAction.class);
		rd.addAction(VoiceRecogAction.NAME, VoiceRecogAction.class);
		rd.addAction(SoundPlayAction.NAME, SoundPlayAction.class);
		rd.addAction(WizardAction.NAME, WizardAction.class);
		rd.addAction(AlarmAction.NAME, AlarmAction.class);
		rd.addAction(ReminderAction.NAME, ReminderAction.class);
		rd.addAction(PowerOff.NAME, PowerOff.class);
		rd.addAction(MoodinfoAction.NAME, MoodinfoAction.class);
		rd.addAction(MoodRecurAction.NAME, MoodRecurAction.class);
		rd.addAction(PathLearnAction.NAME, PathLearnAction.class);
		rd.addAction(PathRecurAction.NAME, PathRecurAction.class);
		rd.addAction(InfraredAction.NAME, InfraredAction.class);
		rd.addAction(DanceAction.NAME, DanceAction.class);
		rd.addAction(XmAction.NAME, XmAction.class);
		rd.addAction(DndAction.NAME, DndAction.class);
		rd.addAction(SimpleProxyAction.NAME, SimpleProxyAction.class);
	}

}
