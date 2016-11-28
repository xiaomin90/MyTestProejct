package cn.flyingwings.robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.flyingwings.robot.logaliservice.AliServerCallback;
import android.content.Context;
import android.text.format.DateFormat;

/**
 *TODO: log日志统计保存、上传-工具类
 *@author min.js
 */
public class LogcatHelper {
	private String TAG = "LogcatHelper";
    public static  LogcatHelper INSTANCE  = null;
    public static  String PATH_LOGCAT_DIR = "/mnt/sdcard/Robot/Logs";
    public  LogDumper mLogDumper = null;
    private Context   mContext   = null;
    public AliServerResultCallBack callback = new AliServerResultCallBack();
    private int       mPId;
    /**
     * 初始化目录
     */
    public static void init(Context context) {
        File file_dir = new File(PATH_LOGCAT_DIR);
        if (!file_dir.exists()) {
        	file_dir.mkdirs();
        }
    }

    /**
     * 单例模式
     * @param context
     * @return
     */
    public static LogcatHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogcatHelper(context);
        }
        return INSTANCE;
    }

    /**
     * 构造器  mpid 进程ID
     * @param context
     */
    private LogcatHelper(Context context) {
        mContext = context;
        mPId = android.os.Process.myPid();
    }

    /**
     * 开始记录log
     */
    public void start() {
        if (mLogDumper == null) {
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT_DIR);
            mLogDumper.start();
        } else {
        	mLogDumper.mRunning = true;
        	mLogDumper.start();
        }
    }

    /**
     * 停止记录log
     */
    public void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
    }

    /**
     * 上传服务器
     */
    public void uptoserver() {
    	
    }
    
    /**
     * 回调通知上传是否成功
     * @author Administrator
     *
     */
    public class AliServerResultCallBack implements AliServerCallback {

		@Override
		public void infoUploadOk(boolean isok) {
			if(isok) {
				RobotDebug.d(TAG, "infoUpload 成功");
			} else {
				RobotDebug.d(TAG, "infoUpload 失败");
			}
			// 重新开始记录文件
			LogcatHelper.getInstance(Robot.getInstance().getContext()).start();
		}
    }
    
    /**
     * 获取log
     * @author min.js
     */
    public class LogDumper extends Thread {
        private Process  logcatProc;
        private BufferedReader mReader = null;
        public  boolean  mRunning = false;
        private String   cmds = null;
        private final    String mPID;
        public  FileOutputStream out = null;
        private List<String> logsMessage = new ArrayList<String>();
        private boolean mLogFileLock = false;
        private String  logFileName;
        
        /**
         * log 文件是否在使用中
         * @return
         */
        public boolean isLogFileLock() {
            return mLogFileLock;
        }

        /**
         * 构造器
         * @param pid
         * @param root_dir
         */
        public LogDumper(String pid, String root_dir) {
            mPID = String.valueOf(pid);
            String needDir = root_dir + "/" + DateFormat.format("yyyy-MM-dd", new Date());
            File mRootDir = new File(root_dir);
            File mdir  = new File(needDir);
            File mFile = new File(needDir+"/log.txt");
            // mkdir 
            if (!mdir.exists()) {
            	// 不存在说明是当天第一次开机，需要删除其他日期的文件。
            	deleteDir(mRootDir); // 删除上级目录下的所有文件
                mdir.mkdirs(); // 创建当天日期的文件
                RobotDebug.d(TAG, "create dir ok : " + mdir.toString());
            }
            // create file
        	if(!mFile.exists()) {
				try {
					RobotDebug.d(TAG, "create file ok : " + mFile.toString());
					mFile.createNewFile(); // 当天创建日志文件
				} catch (IOException e1) {
					e1.printStackTrace();
				}
        	}
        	//FileOutputStream
            try {
                logFileName = mFile.toString();
                out = new FileOutputStream(mFile, true); // append的方式写文件
            } catch (FileNotFoundException e) {
            	RobotDebug.d(TAG, "FileNotFoundException " + logFileName + "  e:" + e.toString());
            }
			/**
			 * 日志等级：*:v , *:d , *:w , *:e , *:I 
			 * 显示当前mPID程序的 E和W等级的日志.
			 * */
             cmds = "logcat *:e *:w *:i *:v *:d | grep \"(" + mPID + ")\"";
        }
        
        /**
         * 删除目录下的所有文件，并删除目录
         * @param dir
         * @return
         */
        private boolean deleteDir(File dir) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                //递归删除目录中的子目录下
                for (int i=0; i<children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            // 目录此时为空，可以删除
            return dir.delete();
        }

        /**
         * log 文件名称
         * @return
         */
        public String getLogFileName() {
            return logFileName;
        }

        /**
         * 停止记录
         */
        public void stopLogs() {
            mRunning = false;
        }

        /**
         * 记录log到文件
         */
        @Override
        public void run() {
            mRunning = true;
            try {
	                logcatProc = Runtime.getRuntime().exec(cmds);
	                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
	                String line = null;
	                while (mRunning && (line = mReader.readLine()) != null) {
			                    if (!mRunning) 
			                        break;
			                    if (line.length() == 0) 
			                    	continue;
			                    synchronized (out) {
			                    	if (out != null) {
				                        if (isLogFileLock()) {
			                                if (line.contains(mPID))
			                                    logsMessage.add(line.getBytes() + "\n");
			                            } else {
			                                if (logsMessage.size() > 0) {
			                                    for (String _log : logsMessage) 
			                                        out.write(_log.getBytes());
			                                    logsMessage.clear();
			                                }
											/**
											**再次过滤日志，筛选当前日志中有 mPID 则是当前程序的日志.
											**/
					                        if (line.contains(mPID)) {
					                            out.write(line.getBytes());
					                            out.write("\n".getBytes());
					                        }
				                       }
			                      }
			                  }
	              }
          } catch (IOException e) {
                 e.printStackTrace();
                 return;
          } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out = null;
                }
            }
        }
    }
}