package com.shikha.stackoverflow.common;

public class StreamTag {
	String rundate;
	String name;
	int count;
	
	public static StreamTag parseTag(String tag, int count) {
		StreamTag t = new StreamTag();
		t.setRundate("2017-02-06 11:12:12.123");
		t.setCount(count);
		t.setName(tag);
		return t;
	}

	public String getName() {
		return name;
	}

	public void setName(String tag) {
		this.name = tag;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getRundate() {
		return rundate;
	}

	public void setRundate(String rundate) {
		this.rundate = rundate;
	}
}
