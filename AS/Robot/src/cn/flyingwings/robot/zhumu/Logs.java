package cn.flyingwings.robot.zhumu;

import android.util.Log;

/**
 * ��������Ӧ�õ�LOG��ӡ
 * 
 * @author cui.li
 * 
 */
public final class Logs {

	private int debugType = DebugType.V;

	public static final class DebugType {
		public static final int V = 0;
		public static final int D = 1;
		public static final int I = 2;
		public static final int W = 3;
		public static final int E = 4;
		public static final int N = 5; // close all debug info
	}

	private String TAG = "";

	/**
	 * ����Ҫ��ӡLOG�����TAG��Ϣ
	 * 
	 * @param TAG
	 *            Ӧ���������İ���������
	 * @param debugType
	 *            ����LOG����߼���
	 */
	public Logs(String TAG) {
		this.TAG = TAG;
	}
	
    /**
     * ����Ҫ��ӡLOG�����TAG��Ϣ
     * @param TAG Ӧ���������İ���������
     * @param debugType ����LOG����߼���
     */
    public Logs(String TAG, int debugType){
    	this.TAG = TAG;
    	this.debugType = debugType;
    }

	public void D(String s) {
		if (debugType <= DebugType.D) {
			Log.d(TAG, s);
		}
	}

	public void E(String s) {
		if (debugType <= DebugType.E) {
			Log.e(TAG, s);
		}
	}

	public void I(String s) {
		if (debugType <= DebugType.I) {
			Log.i(TAG, s);
		}
	}

	public void W(String s) {
		if (debugType <= DebugType.W) {
			Log.w(TAG, s);
		}
	}

	public void V(String s) {
		if (debugType <= DebugType.V) {
			Log.v(TAG, s);
		}
	}
}
