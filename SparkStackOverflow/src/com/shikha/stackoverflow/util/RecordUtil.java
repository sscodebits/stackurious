package com.shikha.stackoverflow.util;

import com.shikha.stackoverflow.model.TagRecord;
import org.w3c.dom.Element;

import com.shikha.stackoverflow.util.ParseUtil;

//Not using this class
public class RecordUtil {
	public static TagRecord parseTag( Element e) {
		TagRecord p = new TagRecord();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setName(e.getAttribute("TagName"));
		p.setCount(ParseUtil.parseLong(e, "Count"));
		return p;
	}
}
