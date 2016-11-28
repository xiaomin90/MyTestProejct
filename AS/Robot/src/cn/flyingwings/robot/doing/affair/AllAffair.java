package cn.flyingwings.robot.doing.affair;


/**
 * 事务注册辅助类<br>
 * 将所有事务通过这个类统一进行注册，方便管理。
 * @author gd.zhang
 *
 */
public class AllAffair {

	public static void initAllAffair(RobotAffairManager am) {
		am.addAffair(AlarmAffair.NAME, AlarmAffair.class);
		am.addAffair(Call.NAME, Call.class);
		am.addAffair(Charge.NAME, Charge.class);
		am.addAffair(ChatAffair.NAME, ChatAffair.class);
		am.addAffair(ClientControl.NAME, ClientControl.class);
		am.addAffair(IdleAffair.NAME, IdleAffair.class);
		am.addAffair(MusicAffair.NAME, MusicAffair.class);
		am.addAffair(PowerOffAffair.NAME, PowerOffAffair.class);
		am.addAffair(RobotInfoAffair.NAME, RobotInfoAffair.class);
		am.addAffair(SeekHome.NAME, SeekHome.class);
		am.addAffair(SetWiFi.NAME, SetWiFi.class);
		am.addAffair(SmartDeviceAffair.NAME, SmartDeviceAffair.class);
		am.addAffair(SoundPlayAffair.NAME, SoundPlayAffair.class);
		am.addAffair(UpdateAffair.NAME, UpdateAffair.class);
		am.addAffair(WizardAffair.NAME, WizardAffair.class);
		am.addAffair(MoodInfoAffair.NAME, MoodInfoAffair.class);
		am.addAffair(MoodRecurAffair.NAME, MoodRecurAffair.class);
		am.addAffair(PathLearnAffair.NAME, PathLearnAffair.class);
		am.addAffair(PathRecurAffair.NAME, PathRecurAffair.class);
		am.addAffair(DanceAffair.NAME, DanceAffair.class);
		am.addAffair(XmAffair.NAME, XmAffair.class);
	}

}
