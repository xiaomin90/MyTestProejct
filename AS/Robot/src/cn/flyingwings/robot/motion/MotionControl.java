package cn.flyingwings.robot.motion;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;
import cn.flyingwings.robot.Robot;
import cn.flyingwings.robot.RobotDebug;
import cn.flyingwings.robot.xmppservice.XmppService;
import android.util.Log;


/**
 * 机器人运动控制<br>
 * @author js.min
 */
public class MotionControl {	
	/**
	 * @Log TAG 
	 * */
	private static final String TAG = "MotionControl";
	
	private final int OpCode = 92001;
	private static final String KEY_MODULE  = "module";
	private static final String KEY_VERSION = "version";
	private static final String KEY_OPCODE  = "opcode";
	private static final String KEY_OPT = "opt";
	private static final String KEY_XPOS = "xpos";
	private static final String KEY_YPOS = "ypos";
	private static final String KEY_ACT = "act";
	private static final String KEY_ANG = "ang";
	private static final String KEY_DISTANCE = "distance";
	
	@SuppressWarnings("unused")
	private String moduleName = "";
	@SuppressWarnings("unused")
	private int version = 0;
	private int opcode = 0;
	private int opt = 0;
	private double xpos = 0.00;
	private double ypos = 0.00;
	private int act = 0;
	@SuppressWarnings("unused")
	private int ang = 0;
	private int distance = 0;
	
	
	private int last_opt = 0;
	private int last_act = 0;
	private int last_oratention = 0;
	
	
	public class Body_Speed {
		public short left_speed = 0;
		public short right_speed = 0;
		public Body_Speed(short left_spd, short right_spd) {
			left_speed =  left_spd;
			right_speed = right_spd;
		}
	}
	
	private static final short[] left_bodySpeed = {(short)25,(short)45,(short)60,(short)15,(short)-25,(short)-15,(short)-60,(short)-45};
	private static final short[] right_bodySpeed = {(short)-25,(short)15,(short)60,(short)45,(short)25,(short)-45,(short)-60,(short)-15};
	private static HashMap<String, Body_Speed> bodyspeedMap;
	private  String json_format = "{\"opt\":%d,\"act\":%d,\"left_speed\":%d,\"right_speed\":%d,\"header_h\":%d,\"header_v\":%d,\"ang\":%d,\"distance\":%d}"; 
	
	private MotionControl()
	{
		bodyspeedMap = new HashMap<String,Body_Speed>();
		bodyspeedMap.put("1", new Body_Speed(left_bodySpeed[0],right_bodySpeed[0]));
		bodyspeedMap.put("2", new Body_Speed(left_bodySpeed[1],right_bodySpeed[1]));
		bodyspeedMap.put("3", new Body_Speed(left_bodySpeed[2],right_bodySpeed[2]));
		bodyspeedMap.put("4", new Body_Speed(left_bodySpeed[3],right_bodySpeed[3]));
		bodyspeedMap.put("5", new Body_Speed(left_bodySpeed[4],right_bodySpeed[4]));
		bodyspeedMap.put("6", new Body_Speed(left_bodySpeed[5],right_bodySpeed[5]));
		bodyspeedMap.put("7", new Body_Speed(left_bodySpeed[6],right_bodySpeed[6]));
		bodyspeedMap.put("8", new Body_Speed(left_bodySpeed[7],right_bodySpeed[7]));
	}
	
	private volatile static MotionControl   instance = null;
	/**
	 * jdk 1.5  双重检查锁定才能够正常达到单例效果
	 * @return MotionControl instance
	 */
	public static  MotionControl getInstance() 
	{  
		if (instance == null) {  
			 synchronized (MotionControl.class) {  
		        if (instance == null) {  
		        	instance = new MotionControl();  
		        }   
			 }
		}  
	    return instance;  
	}  	
	
	
	/**
	 * @function :  判断json消息内容中动作请求,发出响应的动作
	 * @parameter:  json 消息格式,详细可以参考机器人与客户端通信协议
	 * @return   ：  void 
	 * **/
	public void sendMotion(JSONObject json)
	{
		MotionLog.d(TAG,json.toString());
		this.last_opt = this.opt;
		this.last_act = this.act;
		this.moduleName = "";
		this.version = 0;
		this.opcode = 0;
		this.opt = 0;
		this.xpos = 0.00;
		this.ypos = 0.00;
		this.act = 0;
		this.ang = 0;
		try{
			if(!json.isNull(KEY_MODULE))
				this.moduleName = json.getString(KEY_MODULE);
			if(!json.isNull(KEY_VERSION))
				this.version = json.getInt(KEY_VERSION);
			if(!json.isNull(KEY_OPCODE))
				this.opcode = json.getInt(KEY_OPCODE);
			if(!json.isNull(KEY_OPT))
				this.opt  = json.getInt(KEY_OPT);
			if(!json.isNull(KEY_XPOS))
				this.xpos = Double.parseDouble(json.getString(KEY_XPOS));
			if(!json.isNull(KEY_YPOS))
				this.ypos = Double.parseDouble(json.getString(KEY_YPOS));
			if(!json.isNull(KEY_ACT))
				this.act  = json.getInt(KEY_ACT);
			if(!json.isNull(KEY_ANG))
				this.ang  = json.getInt(KEY_ANG);
		}catch(Exception e){
			MotionLog.e(TAG,"in sendMotion json parse failed.");
		}
		if((this.opcode == this.OpCode)  && (this.opt == 1) && ( this.act == 0) ){
			double degree = Math.atan2(ypos, xpos); // [-Math.PI,Math.PI]   
			int oratention = this.getHeaderOrientation(degree);	
			MotionLog.d(TAG, String.format("degree : %.2f  oratention: %d", degree,oratention));
			if(oratention == -1 )
				return;
			if((this.last_oratention !=  oratention) || (this.last_opt != this.opt )){
				this.sendHeaderMoveAction(oratention);
			}
			this.last_oratention = oratention;
		}else if ((this.opcode == this.OpCode)  && (this.opt == 2) && ( this.act == 0)){
			double degree = Math.atan2(ypos, xpos); // [-Math.PI,Math.PI]
			int oratention = this.getBodyOrientation(degree);
			MotionLog.d(TAG, String.format("degree : %.2f  oratention: %d", degree,oratention));
			if(oratention == -1 )
				return;
			if((this.last_oratention !=  oratention) || (this.last_opt != this.opt )) {
				this.sendBodyMoveAction(oratention);
			}
			this.last_oratention = oratention;
		}
		else if( this.act == 1)
		{
			/**
			 * @ 全部停止运动
			 * */
			this.sendStopAction();	
			this.last_opt = 0;
			this.last_act = 0;
			this.last_oratention = 0;
		}
		else if(this.act == 6)
		{
			/**
			 * @ 回中
			 * */
			this.sendHeaderBackMiddleAction();
			this.last_opt = 0;
			this.last_act = 0;
			this.last_oratention = 0;
		}
		else
			MotionLog.e(TAG,"--------opt  or act is not corect--------");
	}
	
	/**
	 * @ function ： 获取脑袋的运动方向 
	 * @ return value :  1.右转  2.抬头 3,左转 4.低头 -1,error
	 * @ parameter ： 在[-PI, PI]之间有效 
	 * **/
	private int getHeaderOrientation(double degree)
	{
		if(degree > Math.PI  || degree < (-1*Math.PI))
		{
			MotionLog.e(TAG, "in header degree is out of range.");
			return -1;
		}
		/**
		 * @ (-pi/4,pi/4]  右转头
		 * @ (pi/4,3*PI/4] 抬头 
		 * @ (3*pi/4,PI] + (-3*PI/4,-PI] 左转头
		 * @ [-PI/4,-3*PI/4]  低头
		 * */
		if( (degree > (Math.PI / -4.00))  &&  ( degree <= (Math.PI / 4.00)) ){
			return 1;
		}
		else if((degree > (Math.PI / 4.00))  &&  ( degree <= ( 3.00* Math.PI / 4.00))){
			return 2;
		}
		else if((degree > (-3.00*Math.PI / 4.00))  &&  ( degree <= ( -1.00* Math.PI / 4.00))){
			return 4;
		}
		return 3;
	}
	
	/**
	 * @ function ： 获取底盘的运动方向 
	 * @ return value :  1.右转  2.右前转  3,前进  4.左前转 5.左转 6.左后转  7.后退  8.右后退 - 1,error
	 * @ parameter ： 在[-PI, PI]之间有效 
	 * **/
	private int getBodyOrientation(double degree)
	{
		if(degree > Math.PI  || degree < (-1*Math.PI))
		{
			MotionLog.e(TAG, "in body degree is out of range.");
			return -1;
		}
		/**
		 * @ (-PI/8,PI/8]  ：    右转          value ： 1
		 * @ (PI/8,3*PI/8] :   右前转      value ： 2
		 * @ (3*PI/8, 5*PI/8] : 前进        value ： 3
		 * @ (5*PI/8, 7*PI/8] : 左前转    value ： 4
		 * @ (7*PI/8, PI], (-7*PI/8,-PI] : 左转  value ：5
		 * @ (-5*PI/8,-7*PI/8] : 左后转  value: 6
		 * @ (-3*PI/8,-5*PI/8] ： 后退     value: 7
		 * @ [-PI/8,-3*PI/8]   ： 右后退 value: 8
		 * */
		
		if( (degree > (Math.PI / -8.00))  &&  ( degree <= (Math.PI / 8.00)) ){
			return 1;
		}
		else if((degree > (Math.PI / 8.00))  &&  ( degree <= ( 3.00* Math.PI / 8.00))){
			return 2;
		}
		else if((degree > (3.00 * Math.PI / 8.00))  &&  ( degree <= ( 5.00* Math.PI / 8.00))){
			return 3;
		}
		else if((degree > (5.00 * Math.PI / 8.00))  &&  ( degree <= ( 7.00* Math.PI / 8.00))){
			return 4;
		}
		else if((degree > (7.00 * Math.PI / 8.00))  &&  ( degree <= ( Math.PI ))   || ( (degree <= (-7.00 * Math.PI / 8.00 ) && ( degree > (-1* Math.PI))))){
			return 5;
		}
		else if((degree < (-5.00 * Math.PI / 8.00))  &&  ( degree >= ( -7.00* Math.PI / 8.00))){
			return 6;
		}
		else if((degree < (-3.00 * Math.PI / 8.00))  &&  ( degree >= ( -5.00* Math.PI / 8.00))){
			return 7;
		}
		else if((degree < (-1.00 * Math.PI / 8.00))  &&  ( degree >= ( -3.00* Math.PI / 8.00))){
			return 8;
		}
		return -1;
	}
	
	/**
	 * @function  : 发送底盘运动请求动作(Action: client_move)
	 * @parameter ： orientation 为8个方向的, 1.右转  2.右前转  3,前进  4.左前转 5.左转 6.左后转  7.后退  8.右后退 
	 * @return    ： true: 发出动作成功,在系统中等待执行    false： 发出动作失败.
	 * */
	@SuppressWarnings("unused")
	private boolean sendBodyMoveAction(int orientation)
	{	
		if(orientation > 8 || orientation < 1)
			return false;
		String str;
		Body_Speed body_speed = bodyspeedMap.get(String.format("%d", orientation));
		MotionLog.d(TAG,"speed_left: " + body_speed.left_speed + "  speed_right : " + body_speed.right_speed);
		str= String.format(this.json_format,this.opt,this.act,body_speed.left_speed,body_speed.right_speed,(short)0,(short)0,(int)0,(int)0);
		JSONObject json = null;
		try {
			json = new JSONObject(str.toString());
			json.put("name", "client_move");
			json.put("subName", "motion_control");
		} catch (JSONException e) {
			e.printStackTrace();
			MotionLog.e(TAG, "in sendBodyMoveAction string not json fomart.");
			return false;
		}
		if(json == null){
			return false;
		}
		Robot.getInstance().getManager().toDo(json.toString());		
		return true;
	}
	
	/**
	 * @function  : 发送头部运动请求动作(Action: client_move) 
	 * @parameter : orientation为四个方向：  1.右转  2.抬头 3,左转 4.低头 
	 * @return    : true： 发出动作成功,在系统中等待执行     false: 发出动作失败
	 * */
	@SuppressWarnings("unused")
	private boolean sendHeaderMoveAction(int orientation)
	{	
		String subName = null;
		String str;
		if(orientation == 1){
			str= String.format(this.json_format,this.opt,this.act,0,0,1,0,0,0);
			subName = "sight_right";
		}
		else if(orientation == 2){
			str = String.format(this.json_format,this.opt,this.act,0,0,0,1,0,0);
			subName = "sight_up";
		}
		else if(orientation == 3){
			str = String.format(this.json_format,this.opt,this.act,0,0,-1,0,0,0);
			subName = "sight_left";
		}
		else if(orientation == 4){
			str = String.format(this.json_format,this.opt,this.act,0,0,0,-1,0,0);
			subName = "sight_down";
		}
		else
			return false;
		JSONObject json = null;
		try {
			json = new JSONObject(str.toString());
			json.put("name", "client_move");
			json.put("subName", subName);
		} catch (JSONException e) {
			e.printStackTrace();
			MotionLog.e(TAG, "in sendHeaderMoveAction string not json fomart.");
			return false;
		}
		if(json == null)
			return false;
		Robot.getInstance().getManager().toDo(json.toString());
		return true;
	}
	
	/**
	 * @function  : 发送停止请求动作(Action: client_move) 
	 * @parameter : void
	 * @return    :  true ： 发出动作成功,在系统中等待执行 false: 发出动作失败
	 * */
	@SuppressWarnings("unused")
	private boolean sendStopAction()
	{
		String str= String.format(this.json_format,this.opt,this.act,(short)0,(short)0,(short)0,(short)0,0,0);
		JSONObject json = null;
		try {
			json = new JSONObject(str.toString());
			json.put("name", "client_move");
			json.put("subName", "motion_stop");
		} catch (JSONException e) {
			e.printStackTrace();
			MotionLog.e(TAG, "in sendStopAction string not json fomart.");
			return false;
		}
		if(json == null)
			return false;
		Robot.getInstance().getManager().toDo(json.toString());
		return true;
	}
	
	/**
	 * @function  : 发送头部回中请求动作(Action: client_move) 
	 * @parameter : void
	 * @return    : true ： 发出动作成功,在系统中等待执行 false: 发出动作失败
	 * */
	@SuppressWarnings("unused")
	private boolean sendHeaderBackMiddleAction()
	{
		String str= String.format(this.json_format,this.opt,this.act,(short)0,(short)0,(short)0,(short)0,0,0);
		JSONObject json = null;
		try {
			json = new JSONObject(str.toString());
			json.put("name", "client_move");
			json.put("subName", "sight_front");
		} catch (JSONException e) {
			e.printStackTrace();
			MotionLog.e(TAG, "in sendHeaderBackMiddleAction string not json fomart.");
			return false;
		}
		if(json == null)
			return false;
		Robot.getInstance().getManager().toDo(json.toString());
		return true;
	}
	
	
	/**
	 * @function  : 发送头部回中请求动作(Action: client_move) 
	 * @parameter : void
	 * @return    : true ： 发出动作成功,在系统中等待执行 false: 发出动作失败
	 * */
	public boolean sendHeaderBackMiddleActionForExtern()
	{
		String str= String.format(this.json_format,0,6,(short)0,(short)0,(short)0,(short)0,0,0);
		JSONObject json = null;
		try {
			json = new JSONObject(str.toString());
			json.put("name", "client_move");
			json.put("subName", "sight_front");
		} catch (JSONException e) {
			MotionLog.e(TAG, "in sendHeaderBackMiddleAction string not json fomart. e: " + e.toString());
			return false;
		}
		Robot.getInstance().getManager().toDo(json.toString());
		
		XmppService xmppService = (XmppService) Robot.getInstance().getService(XmppService.NAME);
		JSONObject data_json = new JSONObject();
		try {
			data_json.put("opcode", "92501");
			data_json.put("result", 0);
			xmppService.sendMsg("R2C", data_json.toString(), 92501);
		} catch (JSONException e) {
			RobotDebug.d(TAG,"sendHeaderBackMiddleActionForExtern json e : " + e.toString());
		}
		MoveDect.getInstance().setHasActMsg();
		return true;
	}
	
}
