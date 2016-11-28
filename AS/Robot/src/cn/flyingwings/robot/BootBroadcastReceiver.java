package cn.flyingwings.robot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 自启动方法
 * @author js.min
 * */

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String action_boot="android.intent.action.BOOT_COMPLETED"; 
 
    @Override
    public void onReceive(Context context, Intent intent) {
       /* if (intent.getAction().equals(action_boot)){ 
            Intent StartIntent=new Intent(context,RobotActivity.class); 
            StartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
            context.startActivity(StartIntent); 
        }*/
    }
}