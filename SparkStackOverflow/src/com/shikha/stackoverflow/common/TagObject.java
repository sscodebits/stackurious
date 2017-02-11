package com.shikha.stackoverflow.common;

import java.util.Collections;
import java.util.List;

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

	public String getName() {
		return name;
	}

	public void setName(String tagName) {
		this.name = tagName;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	String name;
	long count;
	
	public static List<TagObject> flattenTag(TagObject userInput) {
		if (userInput == null || userInput.getId().isEmpty()) {
			return Collections.emptyList();
		} else {
			return Collections.singletonList(userInput);
		}
	}

	
	public static TagObject parseElement(Element e) {
		TagObject p = new TagObject();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			p.setName("null");
			p.setCount(0);
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setName(e.getAttribute("TagName"));
		p.setCount(ParseUtil.parseLong(e, "Count"));
		return p;
	}
}
