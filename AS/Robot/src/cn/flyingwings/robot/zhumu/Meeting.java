package cn.flyingwings.robot.zhumu;

import java.io.Serializable;

public class Meeting implements Serializable {
	private int code;

	private int zcode;

	private String id;

	private String username;
	
	private String mobile;

	private int usertype;

	private String det;

	private String createtime;

	private String createby;

	private String pmi;

	private int role;

	private String email;

	private int isowner;

	private int accounttype;

	private String token;

	public void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}

	public void setZcode(int zcode) {
		this.zcode = zcode;
	}

	public int getZcode() {
		return this.zcode;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}
	
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getMobile() {
		return this.mobile;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsertype(int usertype) {
		this.usertype = usertype;
	}

	public int getUsertype() {
		return this.usertype;
	}

	public void setDet(String det) {
		this.det = det;
	}

	public String getDet() {
		return this.det;
	}

	public void setCreatetime(String createtime) {
		this.createtime = createtime;
	}

	public String getCreatetime() {
		return this.createtime;
	}

	public void setCreateby(String createby) {
		this.createby = createby;
	}

	public String getCreateby() {
		return this.createby;
	}

	public void setPmi(String pmi) {
		this.pmi = pmi;
	}

	public String getPmi() {
		return this.pmi;
	}

	public void setRole(int role) {
		this.role = role;
	}

	public int getRole() {
		return this.role;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return this.email;
	}

	public void setIsowner(int isowner) {
		this.isowner = isowner;
	}

	public int getIsowner() {
		return this.isowner;
	}

	public void setAccounttype(int accounttype) {
		this.accounttype = accounttype;
	}

	public int getAccounttype() {
		return this.accounttype;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getToken() {
		return this.token;
	}

}