package cn.flyingwings.robot.doing.affair;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.doing.action.RobotAction;

public class Charge extends RobotAffair {
	
	public static final String NAME = "charge";
	private BroadcastReceiver receiver;
	private String TAGS = "Charge";
	private boolean isregister = false;
	public static  final String  ChargeOKAction = "cn.flyingwings.robot.chargeOK";
	
	@Override
	public String name() {
		return Charge.NAME;
	}
	
	public Charge(RobotAffairManager am) {
		super(am);
	}
	
	protected void onCreated(RobotAction act) {
		receiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent) {
				  Log.d(TAGS,"apprtcAffair receive : " + ChargeOKAction);
				  String action = intent.getAction();
	              if( action.equals(ChargeOKAction)) 
	            	   stopAffair(true);
			}
		};
		Log.d(TAGS,"ChargeAffair Start ");
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ChargeOKAction); 
		Robot.getInstance().getContext().registerReceiver(receiver,intentFilter); 
		isregister = true;
	}
	
	protected synchronized void onFinished() {
		
		Log.d(TAGS, "ChargeAffair is finished.");
		if(isregister == true) {
			Robot.getInstance().getContext().unregisterReceiver(receiver);
			isregister = false;
		}
	}

}
