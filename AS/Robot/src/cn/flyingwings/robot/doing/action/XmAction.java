/**
 * XmAction.java 2016-7-27
 */
package cn.flyingwings.robot.doing.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.chatService.ChatService;
import cn.flyingwings.robot.doing.affair.XmAffair;
import cn.flyingwings.robot.utils.StringUtil;

import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.SearchAlbumList;
import com.ximalaya.ting.android.opensdk.model.track.SearchTrackList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author WangBaoming
 *
 */
public class XmAction extends RobotAction {
	public final static String NAME = "ximalaya";

	private static final long XM_TIME_OUT = 1000 * 8; // milliseconds

	private JSONObject mParm;

	private boolean mStoped = false;
	
	private Handler mTimeoutHandler;
	private boolean mTimeout = false;
	private Runnable mTimeoutRunnable = new Runnable() {
		
		@Override
		public void run() {
			mTimeout = true;
			Log.d(NAME, "mTimeoutRunnable thread: " + Thread.currentThread().toString());
			Log.e(NAME, "xm request timeout.");
			toSay("抱歉，未找到播放内容", 1000);
			
			stopXmAffair();
		}
	};

	private String mPrompt = "";
	
	public static enum XmPlayMode {
		LIST(0), SINGLE(1);
		
		private int mValue;
		private XmPlayMode(int value) {
			mValue = value;
		}
		
		public int value(){
			return mValue;
		}
		
		public static XmPlayMode valueOf(int value){
			if(1 == value){
				return SINGLE;
			}else{
				return LIST;
			}
		}
	}
	
	@Override
	public int type() {
		return ACTION_TYPE_TASK;
	}
	
	@Override
	public String name() {
		return NAME;
	}

	/**
	 * <pre>
	 * {
	 *   "name": "ximalaya",
	 *   "opt": "play/pause/resume/stop",
	 *   "type": "故事/绕口令/诗词/笑话/搜索",
	 *   "album": "album/sound",
	 *   "content": "白雪公主",
	 *   "author":"李白"
	 * }
	 * </pre>
	 */
	@Override
	public boolean parse(String s) {
		super.parse(s);
		
		try {
			mParm = new JSONObject(s);
			if(!NAME.equals(mParm.optString("name")) ){
				return false;
			}
			
			mParm.remove("name");
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * <pre>
	 * {
	 *   "opt": "play/pause/resume/stop",
	 *   "type": "故事/绕口令/诗词/笑话/搜索",
	 *   "album": "album/sound",
	 *   "content": "白雪公主",
	 *   "author":"李白"
	 * }
	 * </pre>
	 */
	@Override
	public void doing() {
		Log.d(NAME, "doing thread: " + Thread.currentThread().toString());
      	Log.d(NAME+"js", "doing: " + this.toString());
		super.doing();
		String opt = mParm.optString("opt", "");
		
		mTimeoutHandler = new Handler(Looper.getMainLooper());
		final XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());
		if("play".equals(opt)){
			if(xmPlayerManager.isPlaying()){
				xmPlayerManager.stop();
			}
			
			String type = mParm.optString("type", "");
			String content = mParm.optString("content", "");
			String album = mParm.optString("album", "album");
			String author = mParm.optString("author", "");
			
			ChatService chatService = (ChatService) Robot.getInstance().getService(ChatService.NAME);
			if(!type.isEmpty()){
				chatService.ttsControll("正在选取中");
				
				if(!content.equals("")){
					if(!author.equals("")){
						mPrompt += author + ",";
					}
					mPrompt += content;
				}else if(!author.equals("")){
					mPrompt = author + "," + type;
				}
				
				SystemClock.sleep(1000 * 1);
			}			

			Log.d(NAME, "search: " + SystemClock.elapsedRealtime());
			
			if("故事".equals(type)){
				if("album".equals(album)){
					if(content.isEmpty()){
						searchAlbum("故事", true);
					}else{
						content += " 故事";
						searchAlbum(content, false);
					}	
				}else{
					searchTrack(content.isEmpty() ? "故事" : content, true, 6);
				}							

			}else if("绕口令".equals(type)){
				if("album".equals(album)){
					if(content.isEmpty()){
						searchAlbum("绕口令", true);
					}else{
						content += " 绕口令";
						searchAlbum(content, false);
					}
				}else{
					content = content.isEmpty() ? "绕口令" : (content + " 绕口令");
					searchTrack(content.isEmpty() ? "绕口令" : content, true, 6);
				}
				
				
			}else if("诗词".equals(type)){
				
				if("album".equals(album)){
					if(content.isEmpty() && author.isEmpty()){ // 随机唐诗宋词
						Map<String, String> map = new HashMap<String, String>();
						final String q = (1 == new Random().nextInt(2)) ? "唐诗三百首" : "宋词三百首";
						map.put(DTransferConstants.SEARCH_KEY, q);
						map.put(DTransferConstants.CATEGORY_ID, "0");
						map.put(DTransferConstants.PAGE, "1");
						
						mPrompt = q;
						
						mTimeoutHandler.postDelayed(mTimeoutRunnable, XM_TIME_OUT);
						CommonRequest.getSearchedAlbums(map, new IDataCallBack<SearchAlbumList>() {
							
							@Override
							public void onSuccess(SearchAlbumList obj) {
								List<Album> list = obj.getAlbums();
								
								boolean foundAlbum = false;
								for(Album album : list){
									if(q.equals(album.getAlbumTitle())){
										foundAlbum = true;
										
										Map<String, String> map = new HashMap<String, String>();
										
										String albumId = String.valueOf(album.getId());
										map.put(DTransferConstants.ALBUM_ID, albumId);
										
										map.put(DTransferConstants.SORT, "asc");
										map.put(DTransferConstants.PAGE, "1");
										CommonRequest.getTracks(map, new IDataCallBack<TrackList>() {
											
											@Override
											public void onSuccess(TrackList obj) {
												synchronized (XmAction.this) {
													if(mTimeout || mStoped) return;
													mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
													
													List<Track> list = obj.getTracks();
													
													int size = 0;
													if(null != list && (size = list.size()) > 0){
														int start = new Random().nextInt(size);
														notifyXmAffair(XmPlayMode.LIST, size);
														
														xmPlayerManager.setPlayMode(PlayMode.PLAY_MODEL_LIST_LOOP);
														xmPlayerManager.playList(list, start);
														
														ttsPrompt(mPrompt, 300 * mPrompt.length());
													}else{
														toSay("抱歉，未找到播放内容", 1000);
														
														stopXmAffair();
													}
												}											
											}
											
											@Override
											public void onError(int code, String msg) {
							                  	Log.d(NAME, "onError " + code + ": " + msg);
												if(!mTimeout && !mStoped) {
													mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
													toSay("抱歉，未找到播放内容", 1000);	
													
													stopXmAffair();					
												}
											}
										});
										
										break;
									}
									
								}
								
								if(!foundAlbum){
									Log.e(NAME, "not found: 唐诗三百首/宋词三百首");
									if(!mTimeout && !mStoped) {
										mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
										
										SystemClock.sleep(1000 * 2);
										toSay("抱歉，未找到播放内容", 1000);
										
										stopXmAffair();
									}
								}
							}
							
							@Override
							public void onError(int code, String msg) {
			                  	Log.d(NAME, "onError " + code + ": " + msg);		
								if(!mTimeout && !mStoped) {
									mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
									toSay("抱歉，未找到播放内容", 1000);		
									
									stopXmAffair();		
								}
							}
						});
					}else{ // 唐诗宋词： 指定名称, 作者
						searchPoetry(2, 2, content, author);
					}
				}else{
					content = content.isEmpty() ? "诗词" : (content + " 诗词");
					searchTrack(content, true, 6/*34*/);
				}
				
			}else if("笑话".equals(type)){
				
				if("album".equals(album)){
					if(content.isEmpty()){
						searchAlbum("笑话", true);
					}else{
						content += " 笑话";
						searchAlbum(content, false);
					}
				}else{
					content = content.isEmpty() ? "笑话" : (content + " 笑话");
					searchTrack(content, true, 6);
				}				
				
			}else if("搜索".equals(type)){
				if("album".equals(album)){ // album search
					searchAlbum(content, false);
				}else if("sound".equals(album)){ // sound search
					Map<String, String> map = new HashMap<String, String>();
					map.put(DTransferConstants.SEARCH_KEY,  content);
					map.put(DTransferConstants.CATEGORY_ID, "0");
					map.put(DTransferConstants.PAGE, "1");

					mTimeoutHandler.postDelayed(mTimeoutRunnable, XM_TIME_OUT);
					CommonRequest.getSearchedTracks(map, new IDataCallBack<SearchTrackList>() {					

						@Override
						public void onSuccess(SearchTrackList obj) {
							synchronized (XmAction.this) {
								if (mTimeout || mStoped) return;
								mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
								
								List<Track> list = obj.getTracks();
								int size = 0;
								if (null != list && (size = list.size()) > 0) {
									notifyXmAffair(XmPlayMode.SINGLE, 1);

									int start = new Random().nextInt(size);
									xmPlayerManager.playList(list, start);

									Track track = list.get(start);
									if (mPrompt.equals("")) {
										String title = track.getTrackTitle();
										String type = mParm.optString("type",
												"");

										mPrompt = StringUtil.isNullOrEmpty(title) ? type : title;
									}
									ttsPrompt(mPrompt, 300 * mPrompt.length());
								} else {
									toSay("抱歉，未找到播放内容", 1000);

									stopXmAffair();
								}
							}
						}
						
						@Override
						public void onError(int code, String msg) {
		                  	Log.d(NAME, "onError " + code + ": " + msg);
							if(!mTimeout && !mStoped) {
								mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
								toSay("抱歉，未找到播放内容", 1000);	
								
								stopXmAffair();
							}
						}
					});
				}else{
					Log.e(NAME, "must be album/sound");
					toSay("抱歉，未找到播放内容", 1000);						
					stopXmAffair();
				}
			}else{
				Log.e(NAME, "not support type: " + type);
				toSay("抱歉，未找到播放内容", 1000);						
				stopXmAffair();
			}
		}else if("pause".equals(opt)){
			XmAffair.pause();
		}else if("resume".equals(opt)){
			XmAffair.resumePlay();
		}else if("stop".equals(opt)){
			xmPlayerManager.stop();
		}else{
			Log.e(NAME, "not support opt: " + opt);
		}
	}	
	
	/**
	 * 遍历唐诗三百首， 宋词三百首搜索标题包含关键词的诗词。采用递归自调用。
	 * @param type
	 * 		输入2
	 * @param page
	 * 		输入2
	 * @param content
	 * 		待匹配关键词
	 */
	private void searchPoetry(final int type, final int page, final String content, final String author){		
		if(type <= 0 && page <= 0 ){ // not matched content, finished
			if(!mTimeout && !mStoped) toSay("抱歉，未找到播放内容", 1000);
			
			stopXmAffair();
			return ;
		}
		
		Map<String, String> map = new HashMap<String, String>();
		final String q = (2 == type ? "唐诗三百首" : "宋词三百首");
		map.put(DTransferConstants.SEARCH_KEY, q);
		map.put(DTransferConstants.CATEGORY_ID, "0");
		map.put(DTransferConstants.PAGE_SIZE, "150"); // not work
		map.put(DTransferConstants.PAGE, "1");
		
		mTimeoutHandler.postDelayed(mTimeoutRunnable, XM_TIME_OUT);
		CommonRequest.getSearchedAlbums(map, new IDataCallBack<SearchAlbumList>() {
			
			@Override
			public void onSuccess(SearchAlbumList obj) {
				Log.d(NAME, "getSearchedAlbums thread: " + Thread.currentThread().toString());
				List<Album> list = obj.getAlbums();
				
				boolean foundAlbum = false;
				for(Album album : list){
					if(q.equals(album.getAlbumTitle())){
						foundAlbum = true;
						
						Map<String, String> map = new HashMap<String, String>();
						
						String albumId = String.valueOf(album.getId());
						map.put(DTransferConstants.ALBUM_ID, albumId);
						
						map.put(DTransferConstants.SORT, "asc");
						map.put(DTransferConstants.PAGE, String.valueOf(page));
						CommonRequest.getTracks(map, new IDataCallBack<TrackList>() {
							
							@Override
							public void onSuccess(TrackList obj) {
								Log.d(NAME, "getTracks thread: " + Thread.currentThread().toString());
								synchronized (XmAction.this) {
									if (mTimeout || mStoped) return;
									mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
									
									List<Track> list = obj.getTracks();
									if (null == list)
										return; // error: should NOT reach here ...
									boolean found = false;
									int index = 0;
									for (Track track : list) {
										String title = track.getTrackTitle();
										if (!StringUtil.isNullOrEmpty(content) && StringUtil.isNullOrEmpty(author)) {
											if(title.contains(content)){
												XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());

												notifyXmAffair(XmPlayMode.SINGLE, 1);

												xmPlayerManager.setPlayMode(PlayMode.PLAY_MODEL_SINGLE);
												xmPlayerManager.playList(list, index);
												found = true;

												ttsPrompt(mPrompt, 300 * mPrompt.length());

												break;
											}
										}else if (!StringUtil.isNullOrEmpty(author) && StringUtil.isNullOrEmpty(content)) {
											if(title.contains(author)){
												XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());

												notifyXmAffair(XmPlayMode.SINGLE, 1);

												xmPlayerManager.setPlayMode(PlayMode.PLAY_MODEL_SINGLE);
												xmPlayerManager.playList(list, index);
												found = true;

												ttsPrompt(mPrompt, 300 * mPrompt.length());									

												break;
											}	
										}else if(!StringUtil.isNullOrEmpty(content) && !StringUtil.isNullOrEmpty(author)) {
											if(title.contains(content)/* && title.contains(author)*/){
												XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());

												notifyXmAffair(XmPlayMode.SINGLE, 1);

												xmPlayerManager.setPlayMode(PlayMode.PLAY_MODEL_SINGLE);
												xmPlayerManager.playList(list, index);
												found = true;

												ttsPrompt(mPrompt, 300 * mPrompt.length());								

												break;
											}		
										}else{
											Log.e(NAME, "author and content both empty");
										}

										index++;
									}
									if (!found) {
										int page1 = page;
										int type1 = type;

										page1--;
										if (page1 <= 0) {
											type1--;
										}

										if (type1 <= 0 && page1 <= 0) { // not matched content, finished
											if (!mTimeout && !mStoped)
												toSay("抱歉，未找到播放内容", 1000);

											stopXmAffair();
											return;
										}

										if (page1 <= 0) {
											page1 = 2;
										}

										searchPoetry(type1, page1, content, author);
									}
								}
								
							}
							
							@Override
							public void onError(int code, String msg) {
			                  	Log.d(NAME, "onError " + code + ": " + msg);	
								if(!mTimeout && !mStoped) {
									mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
									toSay("抱歉，未找到播放内容", 1000);
									
									stopXmAffair();
								}
							}
						});
						
						break; // album search
					}
					
				}
				
				if(!foundAlbum){
					Log.e(NAME, "not found: " + q);
					
					int page1 = page;
					int type1 = type;

					page1--;
					if (page1 <= 0) {
						type1--;
					}

					if (type1 <= 0 && page1 <= 0) { // not matched content, finished
						if (!mTimeout && !mStoped)
							toSay("抱歉，未找到播放内容", 1000);

						stopXmAffair();
						return;
					}

					if (page1 <= 0) {
						page1 = 2;
					}

					searchPoetry(type1, page1, content, author);
				}
			}
			
			@Override
			public void onError(int code, String msg) {
              	Log.d(NAME, "onError " + code + ": " + msg);
				if(!mTimeout && !mStoped) {
					mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
					toSay("抱歉，未找到播放内容", 1000);	
					
					stopXmAffair();
				}
			}
		});		
	}

	private void searchAlbum(String content, final boolean random) {
		if(StringUtil.isNullOrEmpty(content)) return ;
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(DTransferConstants.SEARCH_KEY, content);
		map.put(DTransferConstants.CATEGORY_ID, "0");
		map.put(DTransferConstants.PAGE, "1");
		
		mTimeoutHandler.postDelayed(mTimeoutRunnable, XM_TIME_OUT);
		CommonRequest.getSearchedAlbums(map, new IDataCallBack<SearchAlbumList>() {
			
			@Override
			public void onSuccess(SearchAlbumList obj) {
				List<Album> list = obj.getAlbums();
				
				int size = 0;
				if(null != list && (size = list.size()) > 0){
					Map<String, String> map = new HashMap<String, String>();
					
					Album album = list.get(random ? new Random().nextInt(size) : 0);
					
					String albumId = String.valueOf(album.getId());
					map.put(DTransferConstants.ALBUM_ID, albumId);
					
					map.put(DTransferConstants.SORT, "asc");
					map.put(DTransferConstants.PAGE, "1");
					
					if(mPrompt.equals("")){
						String title = album.getAlbumTitle();
						String type = mParm.optString("type", "");
						
						mPrompt = StringUtil.isNullOrEmpty(title) ? type : title;
					}
					CommonRequest.getTracks(map, new IDataCallBack<TrackList>() {
						
						@Override
						public void onSuccess(TrackList obj) {
							synchronized (XmAction.this) {
			                  	Log.d(NAME+"js", "onSuccess1");
								if (mTimeout || mStoped) return;
			                  	Log.d(NAME+"js", "onSuccess2: " + XmAction.this.toString());
								mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
								
								List<Track> list = obj.getTracks();
								int size = 0;
								if (null != list && (size = list.size()) > 0) {
									ttsPrompt(mPrompt, 300 * mPrompt.length());

									notifyXmAffair(XmPlayMode.LIST, size);

									XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());
									xmPlayerManager.playList(list, 0);
				                  	Log.d(NAME+"js", "playList ed: " + XmAction.this.toString());
								} else {
									toSay("抱歉，未找到播放内容", 1000);

									stopXmAffair();
								}
							}
						}
						
						@Override
						public void onError(int code, String msg) {
		                  	Log.d(NAME, "onError " + code + ": " + msg);
							if(!mTimeout && !mStoped) {
								mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
								toSay("抱歉，未找到播放内容", 1000);	
								
								stopXmAffair();					
							}
						}
					});
				}else{
					if(!mTimeout && !mStoped) {
						mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
						
						SystemClock.sleep(1000 * 2);
						toSay("抱歉，未找到播放内容", 1000);
						
						stopXmAffair();
					}
				}
			}
			
			@Override
			public void onError(int code, String msg) {
		      	Log.d(NAME, "onError " + code + ": " + msg);	
				if(!mTimeout && !mStoped) {
					mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
					toSay("抱歉，未找到播放内容", 1000);		
					
					stopXmAffair();			
				}
			}
		});
	}
	

	private void searchTrack(String content, final boolean random, int categoryId) {
		if(StringUtil.isNullOrEmpty(content)) return ;
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(DTransferConstants.SEARCH_KEY, content);
		map.put(DTransferConstants.CATEGORY_ID, String.valueOf(categoryId));
		map.put(DTransferConstants.PAGE, "1");

		mTimeoutHandler.postDelayed(mTimeoutRunnable, XM_TIME_OUT);
		CommonRequest.getSearchedTracks(map, new IDataCallBack<SearchTrackList>() {					

			@Override
			public void onSuccess(SearchTrackList obj) {
				synchronized (XmAction.this) {
					if (mTimeout || mStoped) return;
					mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
					
					List<Track> list = obj.getTracks();
					int size = 0;
					if (null != list && (size = list.size()) > 0) {
						notifyXmAffair(XmPlayMode.SINGLE, 1);
						int start = random ? new Random().nextInt(size) : 0;

						XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());
						xmPlayerManager.playList(list, start);

						Track track = list.get(start);
						if (mPrompt.equals("")) {
							String title = track.getTrackTitle();
							String type = mParm.optString("type", "");

							mPrompt = StringUtil.isNullOrEmpty(title) ? type : title;
						}
						ttsPrompt(mPrompt, 300 * mPrompt.length());

					} else {
						toSay("抱歉，未找到播放内容", 1000);

						stopXmAffair();
					}
				}
			}
			
			@Override
			public void onError(int code, String msg) {
		      	Log.d(NAME, "onError " + code + ": " + msg);
				if(!mTimeout && !mStoped) {
					mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
					toSay("抱歉，未找到播放内容", 1000);
					
					stopXmAffair();
				}
			}
		});
	}
	
	/**
	 * stop Xm affair
	 */
	private void stopXmAffair(){
		Robot.getInstance().getContext().sendBroadcast(new Intent(XmAffair.XM_ACTION_STOP_AFFAIR));
	}

	private void notifyXmAffair(XmPlayMode mode, int listSize){
		Log.d(NAME, "play: " + SystemClock.elapsedRealtime());
		
		Intent intent = new Intent(XmAffair.XM_ACTION);
		intent.putExtra(XmAffair.XM_EXTRA_PLAY_MODE, mode.value());
		intent.putExtra(XmAffair.XM_EXTRA_LIST_LEN, listSize);
		
		Robot.getInstance().getContext().sendBroadcast(intent);
	}
	
	private void ttsPrompt(String content, int ms){
		if(StringUtil.isNullOrEmpty(content)) return ;
		
		ChatService chatService = (ChatService) Robot.getInstance().getService(ChatService.NAME);
		chatService.ttsControll(content);
		
		SystemClock.sleep(ms);
	}
	
	private void toSay(String content, int ms){
		if(StringUtil.isNullOrEmpty(content)) return ;
		
		try {
			JSONObject todo = new JSONObject()
								.put("name", SayAction.NAME)
								.put("content", content);
			Robot.getInstance().getManager().toDo(todo.toString());
			
			SystemClock.sleep(500);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void stop(){
      	Log.d(NAME, "act stop1");
		mStoped = true;
		if(null != mTimeoutHandler){
			mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
		}
		
		XmPlayerManager xmPlayerManager = XmPlayerManager.getInstance(Robot.getInstance().getContext());
		xmPlayerManager.stop();
		xmPlayerManager.resetPlayList();
      	Log.d(NAME, "act stop2: " + this.toString());
	}
}
