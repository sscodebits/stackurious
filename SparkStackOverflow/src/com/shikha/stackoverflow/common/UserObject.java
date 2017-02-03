package com.shikha.stackoverflow.common;

import org.w3c.dom.Element;

import com.shikha.stackoverflow.util.ParseUtil;

/**
 * User object
 * <row Id="24" Reputation="982" CreationDate="2014-09-23T18:29:49.000" DisplayName="artagnon" LastAccessDate="2015-11-28T15:18:18.633" WebsiteUrl="http://artagnon.com" Location="New York, United States" AboutMe="&lt;p&gt;Programmer who likes low-level details.&lt;/p&gt;&#xA;" Views="0" UpVotes="0" DownVotes="0" Age="28" AccountId="49024" />
 * @author shikha
 *
 */

public class UserObject {
	String id;
	long reputation;
	String creation_date;
	String display_name;
	long views;
	long upvotes;
	long downvotes;
	String location;

	
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public long getReputation() {
		return reputation;
	}


	public void setReputation(long l) {
		this.reputation = l;
	}


	public String getCreation_date() {
		return creation_date;
	}


	public void setCreation_date(String creation_date) {
		this.creation_date = creation_date;
	}


	public String getDisplay_name() {
		return display_name;
	}


	public void setDisplay_name(String display_name) {
		this.display_name = display_name;
	}


	public long getViews() {
		return views;
	}


	public void setViews(long views) {
		this.views = views;
	}


	public long getUpvotes() {
		return upvotes;
	}


	public void setUpvotes(long upvotes) {
		this.upvotes = upvotes;
	}


	public long getDownvotes() {
		return downvotes;
	}


	public void setDownvotes(long downvotes) {
		this.downvotes = downvotes;
	}


	public String getLocation() {
		return location;
	}


	public void setLocation(String location) {
		this.location = location;
	}


	public static UserObject parseElement(Element e) {
		UserObject p = new UserObject();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			p.setDisplay_name("null");
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setCreation_date(e.getAttribute("CreationDate"));
		p.setReputation(ParseUtil.parseLong(e, "Reputation"));
		p.setDisplay_name(e.getAttribute("DisplayName"));
		p.setUpvotes(ParseUtil.parseLong(e, "UpVotes"));
		p.setDownvotes(ParseUtil.parseLong(e, "DownVotes"));
		p.setLocation(e.getAttribute("Location"));
		p.setViews(ParseUtil.parseLong(e, "Views"));
		return p;
	}
}
