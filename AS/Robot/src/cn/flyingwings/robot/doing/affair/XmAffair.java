/**
 * XmAffair.java 2016-7-27
 */
package cn.flyingwings.robot.doing.affair;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis;
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.doing.action.RobotAction;
import cn.flyingwings.robot.doing.action.SayAction;
import cn.flyingwings.robot.doing.action.XmAction;
import cn.flyingwings.robot.doing.action.XmAction.XmPlayMode;

/**
 * @author WangBaoming
 *
 */
public class XmAffair extends RobotAffair {
	public final static String NAME = "ximalaya";
	
    private final static String APP_SECRET = "bf5d7c4fe31d3141368bb7377762e796";

    public final static String XM_ACTION_STOP_AFFAIR = "cn.flyingwings.robot.action.xm.stop_affair";
    
    public final static String XM_ACTION = "cn.flyingwings.robot.action.xm";	
    public final static String XM_EXTRA_PLAY_MODE = "cn.flyingwings.robot.xm.extra.mode";
    public final static String XM_EXTRA_LIST_LEN = "cn.flyingwings.robot.xm.extra.list_len";

    private XmPlayerManager mPlayerManager;
    private CommonRequest mXimalaya;
    private Context mContext;
    
    private XmPlayMode mPlayMode = XmPlayMode.LIST;
    private static int mListLen = 1;
    private static int mSoundCount = 0;
    
	private IXmPlayerStatusListener mPlayerStatusListener;
	private IXmAdsStatusListener mAdsListener;
	
	private RobotAction mCurrAction;
	private boolean mFinished = false;
	
	private static boolean sIsPaused = false;
	
	private static List<Track> sPlayList;
	private static int sCurrIndex;

    private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(XM_ACTION.equals(action)){
				mSoundCount = 0;
				mListLen = intent.getIntExtra(XM_EXTRA_LIST_LEN, 1);
				mPlayMode = XmPlayMode.valueOf(intent.getIntExtra(XM_EXTRA_PLAY_MODE, XmPlayMode.LIST.value()));
			}else if(XM_ACTION_STOP_AFFAIR.equals(action)){
				stopAffair(true);
			}
		}
    	
    };


	public XmAffair(RobotAffairManager am) {
		super(am);
	}

	@Override
	public String name() {
		return NAME;
	}
	
	private XmPlayMode getPlayMode(){
		return mPlayMode;
	}
	
	private boolean hasNextSound(){
		return mSoundCount < mListLen;
	}
	
	@Override
	protected void onCreated(RobotAction act) {
		super.onCreated(act);
		mContext = Robot.getInstance().getContext();
		
		mXimalaya = CommonRequest.getInstanse();
		mXimalaya.init(mContext, APP_SECRET);
		mXimalaya.setDefaultPagesize(160);
		
		mPlayerManager = XmPlayerManager.getInstance(mContext);		
		mPlayerStatusListener = new IXmPlayerStatusListener() {

			@Override
			public void onSoundSwitch(PlayableModel laModel, PlayableModel curModel) {
				Log.e(NAME, "onSoundSwitch");
				Log.d(NAME, "onSoundSwitch thread: " + Thread.currentThread().toString());
				if(mFinished){
					Log.e(NAME, "XmAffair finished");
//					mPlayerManager.stop();
					mPlayerManager.resetPlayList();
				}
			}

			@Override
			public void onSoundPrepared() {
				Log.e(NAME, "onSoundPrepared");
			}

			@Override
			public void onSoundPlayComplete() {
				Log.e(NAME, "onSoundPlayComplete");
				/* some bug, the line not work correctly */
/*						PlayMode playMode = mPlayerManager.getPlayMode();
				
				switch (playMode) {
				case PLAY_MODEL_LIST:
					if(!mPlayerManager.hasNextSound()){
						try {
							JSONObject todo = new JSONObject()
												.put("name", SayAction.NAME)
												.put("content", "您点播的内容已播放完毕");
							Robot.getInstance().getManager().toDo(todo.toString());
							
							SystemClock.sleep(1000 * 1);
						} catch (JSONException e) {
							e.printStackTrace();
						}

						stopAffair(true);
					}
					
					break;
				case PLAY_MODEL_SINGLE:
					try {
						JSONObject todo = new JSONObject()
											.put("name", SayAction.NAME)
											.put("content", "您点播的内容已播放完毕");
						Robot.getInstance().getManager().toDo(todo.toString());

						SystemClock.sleep(1000 * 1);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					stopAffair(true);
					break;

				default:
					break;
				}						
*/						
				mSoundCount++;
				XmPlayMode playMode = getPlayMode();
				
				switch (playMode) {
				case LIST:
					if(!hasNextSound()){
						try {
							JSONObject todo = new JSONObject()
												.put("name", SayAction.NAME)
												.put("content", "您点播的内容已播放完毕");
							Robot.getInstance().getManager().toDo(todo.toString());
							
							SystemClock.sleep(500);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						sIsPaused = false;
						stopAffair(true);
					}
					
					break;
				case SINGLE:
					try {
						JSONObject todo = new JSONObject()
											.put("name", SayAction.NAME)
											.put("content", "您点播的内容已播放完毕");
						Robot.getInstance().getManager().toDo(todo.toString());

						SystemClock.sleep(500);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					sIsPaused = false;
					stopAffair(true);
					break;

				default:
					break;
				}						

			}

			@Override
			public void onPlayStop() {
				Log.e(NAME, "onPlayStop");
			}

			@Override
			public void onPlayStart() {
				Log.e(NAME, "onPlayStart");
			}

			@Override
			public void onPlayProgress(int currPos, int duration) {
				// Log.e(NAME, "onPlayProgress " + currPos + ", " + duration);
			}

			@Override
			public void onPlayPause() {
				Log.e(NAME, "onPlayPause");
			}

			@Override
			public boolean onError(XmPlayerException exception) {
				Log.e(NAME, "onError");
				return false;

			}

			@Override
			public void onBufferingStop() {
				Log.e(NAME, "onBufferingStop");
			}

			@Override
			public void onBufferingStart() {
				Log.e(NAME, "onBufferingStart");
			}

			@Override
			public void onBufferProgress(int percent) {
				// Log.e(NAME, "onBufferProgress " + percent);
			}

		};
		mAdsListener = new IXmAdsStatusListener() {
			
			@Override
			public void onStartPlayAds(Advertis arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartGetAdsInfo() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetAdsInfo(AdvertisList arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onError(int arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onCompletePlayAds() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAdsStopBuffering() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAdsStartBuffering() {
				// TODO Auto-generated method stub
				
			}
		};
		
		mPlayerManager.init();
		mPlayerManager.addPlayerStatusListener(mPlayerStatusListener);
		mPlayerManager.addAdsStatusListener(mAdsListener);

		mPlayerManager.getPlayerStatus();
		
		IntentFilter intentFilter = new IntentFilter(XM_ACTION);
		intentFilter.addAction(XM_ACTION_STOP_AFFAIR); 
		Robot.getInstance().getContext().registerReceiver(receiver, intentFilter);
		
	}
	
	@Override
	protected void onAction(RobotAction act) {
		super.onAction(act);
		if(act instanceof XmAction){
			if(null != mCurrAction && mCurrAction instanceof XmAction){
				((XmAction) mCurrAction).stop();
			}

			mCurrAction = act;
		}
	}
	
	@Override
	protected void onFinished() {
		Log.d(NAME, "onFinished thread: " + Thread.currentThread().toString());
		mFinished = true;
		
		if(null != mPlayerManager){
			mPlayerManager.removePlayerStatusListener(mPlayerStatusListener);
			mPlayerManager.removeAdsStatusListener(mAdsListener);
			
			if(mPlayerManager.isPlaying()){
				sIsPaused = true;
				sPlayList = mPlayerManager.getPlayList();
				sCurrIndex = mPlayerManager.getCurrentIndex();
			}
			
			mPlayerManager.stop();
			mPlayerManager.resetPlayList();
			mPlayerManager.release();
			mPlayerManager = null;
		}
		
		Robot.getInstance().getContext().unregisterReceiver(receiver);
		
		if(null != mCurrAction && mCurrAction instanceof XmAction){
			((XmAction) mCurrAction).stop();
		}
		
		super.onFinished();
	}

	public static boolean isPlaying(){
		return XmPlayerManager.getInstance(Robot.getInstance().getContext()).isPlaying();
	}
	
	public static boolean isPaused(){
		return sIsPaused;
	}	

	public static void stopXmPlayer(){
		sIsPaused = false;
		XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());
		xmPlayerManager.stop();
		xmPlayerManager.resetPlayList();
	}

	public static void pause() {
		XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());	
		
		if(xmPlayerManager.isPlaying()){
			sIsPaused = true;
			sPlayList = xmPlayerManager.getPlayList();
			sCurrIndex = xmPlayerManager.getCurrentIndex();
			
			xmPlayerManager.pause();
		}
	}

	public static void resumePlay() {
		if(sIsPaused){
			XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());			
			xmPlayerManager.playList(sPlayList, sCurrIndex);
		}
	}
}
