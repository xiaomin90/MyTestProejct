package cn.flyingwings.robot.utils;

import java.util.List;

public class MusicBean {

	public String result;
	public List<SongInfo> data;
	public class SongInfo {
		public String song_file;
		public int resource_id;
		public String song_name;
		public String singer_name;
	}
}
