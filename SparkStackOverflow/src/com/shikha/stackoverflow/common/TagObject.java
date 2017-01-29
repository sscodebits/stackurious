package com.shikha.stackoverflow.common;

import org.w3c.dom.Element;

import com.shikha.stackoverflow.util.ParseUtil;

public class TagObject {
	String id;
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	String tagName;
	long count;
	
	public static TagObject parseElement(Element e) {
		TagObject p = new TagObject();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setTagName(e.getAttribute("TagName"));
		p.setCount(ParseUtil.parseLong(e, "Count"));
		return p;
	}
}
