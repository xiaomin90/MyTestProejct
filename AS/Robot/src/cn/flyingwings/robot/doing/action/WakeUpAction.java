package cn.flyingwings.robot.doing.action;

import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.chatService.ModuleController;
import cn.flyingwings.robot.chatService.ModuleController.State;
import cn.flyingwings.robot.chatService.SpeechRecognize;
import cn.flyingwings.robot.service.MusicPlayService;

public class WakeUpAction extends RobotAction {

	public static final String NAME = "voice_wakeup";

	@Override
	public int type() {
		// TODO Auto-generated method stub
		return RobotAction.ACTION_TYPE_TASK;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	public boolean parse(String s) {
		// TODO Auto-generated method stub
		return super.parse(s);
	}

	@Override
	public void doing() {
		// TODO Auto-generated method stub
		super.doing();
		stopMusic();
		
		//SoundPlay.getInstance().cancel();
		//stopRtcAudio();
		Log.e("voice_module", "wakeupAction doing");
		try {
			playAudio();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void stopMusic(){
		MusicPlayService musicPlayService = (MusicPlayService) Robot.getInstance().getService(MusicPlayService.NAME);
		musicPlayService.stopMusicPlay();
	}
	
	private void playAudio() throws IOException{
		Context context = Robot.getInstance().getContext();
		AssetFileDescriptor fileDescriptor = context.getAssets().openFd("sound/wake_up.wav");
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setDataSource(
				fileDescriptor.getFileDescriptor(),
				fileDescriptor.getStartOffset(),
				fileDescriptor.getLength());
		mediaPlayer.prepare();

		mediaPlayer.start();
		
		try {
			Thread.sleep(mediaPlayer.getDuration());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mediaPlayer.reset();
		mediaPlayer.release();
		SpeechRecognize.initFailureCount();
		ModuleController.getInstance().changeState(State.SPEECHERCOG, null);
	}
}
