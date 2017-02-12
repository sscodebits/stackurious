package com.shikha.stackoverflow.common;


public class StreamPost {
	String id=null;
	String post_type_id;
	String parent_id;
	String accepted_answer_id;
	String creation_date;
	String title;
	String tags;
	public static StreamPost parseString(String line) {
		StreamPost post = new StreamPost();
		if (line ==null || line.isEmpty()) {
			post.setId("################## ERROR Line ");
			return post;
		}
        //Post Stream        
		//newid,postTypeId=1,creationDate,tags,title,
		//id,postTypeId=2,parentId,creationDate,tags,title,AcceptedAnswer=0/1

		String[] parsedString = line.split(",");
		if (parsedString.length >= 6 ) {
			post.setId(parsedString[0]);
			post.setPost_type_id(parsedString[1]);
			post.setParent_id(parsedString[2]);
			post.setCreation_date(parsedString[3]);
			post.setTags(parsedString[4]);
			post.setTitle(parsedString[5]);
			if (parsedString.length >= 7) {
				post.setAccepted_answer_id(parsedString[6]);
			}
		} else {
			post.setId("#"+line+"#");
		}
	
		return post;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPost_type_id() {
		return post_type_id;
	}
	public void setPost_type_id(String post_type_id) {
		this.post_type_id = post_type_id;
	}
	public String getParent_id() {
		return parent_id;
	}
	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}
	public String getAccepted_answer_id() {
		return accepted_answer_id;
	}
	public void setAccepted_answer_id(String accepted_answer_id) {
		this.accepted_answer_id = accepted_answer_id;
	}
	public String getCreation_date() {
		return creation_date;
	}
	public void setCreation_date(String creation_date) {
		this.creation_date = creation_date;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}

}
