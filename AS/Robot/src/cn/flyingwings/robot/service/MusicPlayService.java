package cn.flyingwings.robot.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.text.TextUtils;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.FaceService.FaceService;
import cn.flyingwings.robot.FaceService.FaceType;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.chatService.SpeechObserver;
import cn.flyingwings.robot.chatService.SpeechObserver.SpeechObservable;
import cn.flyingwings.robot.chatService.TuRingUtils.WebCallBack;
import cn.flyingwings.robot.doing.affair.MusicAffair;
import cn.flyingwings.robot.utils.MusicBean;
import cn.flyingwings.robot.utils.MusicBean.SongInfo;
import cn.flyingwings.robot.utils.MusicGet;
import cn.flyingwings.robot.xmppservice.XmppService;

public class MusicPlayService extends RobotService {

	public static final String NAME = "MusicPlayService";
	private Context context;
	public static final String TAG = "MusicAffair";
	public static final int PAGE_SIZE = 20;
	public int mPageNo = 1;
	public static boolean isAlive = false;
	public static final String MUSIC_ACTION = "cn.flyingwings.robot.MusicPlay";

	private MediaPlayer mediaPlayer;

	private String localSongName = "";
	private String localartistName = "";
	private String songUrl = "";
	private volatile static boolean isPlaying = false;
	public static boolean loopPlay = false;
	public static String loopArtist = "";
	private static volatile boolean isPuase = false;

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		super.start();

		context = Robot.getInstance().getContext();
		IntentFilter intentFilter = new IntentFilter(MUSIC_ACTION);
		context.registerReceiver(musicReceiver, intentFilter);
	}

	/**
	 * 
	 * @param songName
	 *            歌曲名
	 * @param artist
	 *            演唱者
	 * @param flag
	 *            true: 代表来自机器人本体的音乐播放 ， false:代表通过手机客户端
	 */
	public void playMusic(String songName, String artist) {
		// puaseRecognize();

		// 停止之前所有的音乐播放
		// stopMusicPlay();
		MusicGet.doRequest(artist, songName, XmppService.conSSessionID, webCallBack);
		return;
	}

	public void keepPlay() {
		isPuase = false;
		if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
			mediaPlayer.start();
			JSONObject data = new JSONObject();
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			try {
				data.put("opcode", "93602");
				data.put("status", "playing");
				data.put("song_name", localSongName);
				data.put("author_name", localartistName);
				xmpp.sendMsg("R2C", data.toString(), 93602);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			setMusicFlag(false);
		}
	}

	private void prePlayMusic(String songName, String artist) {
		String content = null;
		SpeechObserver.getInstance().remove();

		content = artist + "," + songName;

		Log.i(TAG, "开始注册： url ： " + songUrl);
		SpeechObserver.getInstance().regist(new MySpeechObservable(songUrl));

		Robot.getInstance().getManager()
				.toDo(String.format("{\"name\":\"say\", \"subName\":\"null\", \"content\":\"%s\"}", content));
	}

	private WebCallBack webCallBack = new WebCallBack() {

		@Override
		public void onSuccess(Object... args) {
			MusicBean musicBean = (MusicBean) args[0];
			if (musicBean != null && !musicBean.data.isEmpty()) {
				// SongInfo info = randomChoice(musicBean.data);
				SongInfo info = musicBean.data.get(0);
				try {
					localSongName = info.song_name;
					localartistName = info.singer_name;
					songUrl = info.song_file;
					JSONObject data = new JSONObject();
					XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
					data.put("opcode", "93602");
					data.put("status", "playing");
					data.put("song_name", localSongName);
					data.put("author_name", localartistName);
					xmpp.sendMsg("R2C", data.toString(), 93602);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (mediaPlayer != null && mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
					mediaPlayer.reset();
				}
				prePlayMusic(info.song_name, info.singer_name);
			}
		}

		@Override
		public void onError() {
			setMusicFlag(false);
			try {
				Robot.getInstance().getManager()
						.toDo(new JSONObject().put("name", "say").put("content", "抱歉，未找到播放内容").toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	};

	private void appClient(String operation, String opcode) {
		if (operation.equals("start")) {

			// FaceService.action_faceCB.changeFace(FaceType.PLAYING_MUSIC);
			/**
			 * stop music
			 */
			if (mediaPlayer != null && !TextUtils.isEmpty(localSongName)) {
				mediaPlayer.start();
				new MyAppCallBack(operation, opcode).onSuccess(localSongName, localartistName, "playing");
			} else {
				playMusic("", "");
			}
			loopPlay = true;
			loopArtist = "";
			// MyAppCallBack mAppCallBack = new MyAppCallBack(operation, opcode,
			// );

		} else if (operation.equals("next")) {
			if (mediaPlayer != null) {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
					// FaceService.action_faceCB.changeFace(FaceType.READY_MODE);
				}
			}
			playMusic("", "");
			loopPlay = true;
		} else if (operation.equals("stop")) {

			/**
			 * stop music
			 */
			if (mediaPlayer != null) {
				new MyAppCallBack(operation, opcode).onSuccess(localSongName, localartistName, "stop");
				if(mediaPlayer.isPlaying()){
					isPuase = true;
					mediaPlayer.pause();
				}
				
			} else {
				new MyAppCallBack(operation, opcode).onSuccess(localSongName, localartistName, "stop");
			}
			loopPlay = false;
			setMusicFlag(false);

		} else if (operation.equals("status")) {
			MyAppCallBack appCallBack = new MyAppCallBack(operation, opcode);
			if (mediaPlayer != null) {
				if (mediaPlayer.isPlaying()) {
					appCallBack.onStatus(localSongName, localartistName, "playing");
				} else {
					appCallBack.onStatus(localSongName, localartistName, "stop");
				}
			} else {
				appCallBack.onStatus(localSongName, localartistName, "stop");
			}
		}
	}

	class MyAppCallBack implements AppCallBack {

		private String opt;
		private String opcode;

		public MyAppCallBack(String opt, String opcode) {
			// TODO Auto-generated constructor stub
			this.opt = opt;
			this.opcode = opcode;
		}

		@Override
		public void onSuccess(String musicTitle, String musicArtist, String status) {
			// TODO Auto-generated method stub
			JSONObject jsonData = new JSONObject();
			try {
				jsonData.put("opcode", opcode);
				jsonData.put("result", 0);
				jsonData.put("song_name", musicTitle);
				jsonData.put("author_name", musicArtist);
				jsonData.put("status", status);
				jsonData.put("error_msg", "");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG, "send to app: " + jsonData.toString());
			XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			xmppService.sendMsg("R2C", jsonData.toString(), 93601);
		}

		/**
		 * 为了区别查询状态
		 * 
		 * @param musicTitle
		 * @param musicArtist
		 * @param status
		 */
		public void onStatus(String musicTitle, String musicArtist, String status) {
			// TODO Auto-generated method stub
			JSONObject jsonData = new JSONObject();
			try {
				jsonData.put("opcode", opcode);
				if (musicTitle != null && musicTitle.length() > 0)
					jsonData.put("song_name", musicTitle);
				if (musicArtist != null && musicArtist.length() > 0)
					jsonData.put("author_name", musicArtist);
				jsonData.put("status", status);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			xmppService.sendMsg("R2C", jsonData.toString(), 93602);
		}

		public String getOpt() {
			return opt;
		}

		public String getOpcode() {
			return opcode;
		}

		@Override
		public void onError(String errorMsg) {
			// TODO Auto-generated method stub

		}
	};

	interface AppCallBack {
		public void onSuccess(String musicTitle, String musicArtist, String status);

		public void onError(String errorMsg);
	}

	private BroadcastReceiver musicReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String opt = intent.getStringExtra("opt");
			String opcode = intent.getStringExtra("opcode");

			Log.i(TAG, "客户端操作" + "opt: " + opt + "  opcode: " + opcode);

			if (TextUtils.isEmpty(opt)) {
				appClient("status", opcode);
			} else {
				appClient(opt, opcode);
			}
		}
	};

	private void playMusic(String url, int seek) {
		if (TextUtils.isEmpty(url)) {
			setMusicFlag(false);
			return;
		}
		
		String affair = Robot.getInstance().getManager().getCurrentAffair();
		if(affair == null || !affair.equals(MusicAffair.NAME)){
			return ;
		}

		if (mediaPlayer != null) {
			mediaPlayer.reset();
			mediaPlayer.release();
		}
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setOnCompletionListener(onCompletionListener);
		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.prepare();

			if (seek > 0) {
				mediaPlayer.seekTo(seek);
			}
			
			if(isPlaying){
				mediaPlayer.start();	
			}else {
				mediaPlayer.reset();
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			setMusicFlag(false);
		}
	}

	private OnCompletionListener onCompletionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			if (loopPlay) {
				playMusic("", loopArtist);
			} else {
				loopArtist = "";
				loopPlay = false;
				setMusicFlag(false);
				finishPlay();
				Robot.getInstance().getContext().sendBroadcast(new Intent(MusicAffair.action_finish_music));
			}
		}
	};

	private boolean localIsPalying() {
		if (mediaPlayer == null) {
			setMusicFlag(false);
			return false;
		}
		boolean isPlaying = false;

		try {
			isPlaying = mediaPlayer.isPlaying();
		} catch (Exception e) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		return isPlaying;
	}

	public boolean isPlaying() {
		return isPlaying || localIsPalying();
	}

	public static synchronized void setMusicFlag(boolean flag) {
		isPlaying = flag;
	}

	class MySpeechObservable implements SpeechObservable {

		private String musicUrl;

		public MySpeechObservable(String musicUrl) {
			this.musicUrl = musicUrl;
		}

		@Override
		public void update() {
			// TODO Auto-generated method stub
			Log.i("kk", "导致音乐播放开始" + songUrl);
			playMusic(musicUrl, 0);
		}

	}

	public void stopMusicPlay() {
		Log.i("kk", "music 被打断");

		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPuase = true;
		}
		setMusicFlag(false);
		JSONObject data = new JSONObject();

		try {
			XmppService xmpp = (XmppService) Robot.getInstance().getService(XmppService.NAME);
			data.put("opcode", "93602");
			data.put("status", "stop");
			data.put("song_name", localSongName);
			data.put("author_name", localartistName);
			xmpp.sendMsg("R2C", data.toString(), 93602);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public synchronized void finishPlay() {
		isPuase = false;
		loopPlay = false;
		loopArtist = "";
		songUrl = null;
		setMusicFlag(false);
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	public static boolean isPuase() {
		return isPuase;
	}

}
