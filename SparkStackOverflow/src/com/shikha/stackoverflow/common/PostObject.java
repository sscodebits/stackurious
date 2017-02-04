package com.shikha.stackoverflow.common;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.shikha.stackoverflow.util.ParseUtil;

public class PostObject {
	String id;
	String post_type_id;
	String parent_id;
	String accepted_answer_id;
	String creation_date;
	long score;
	long view_count;
	String body;
	String owner_user_id;
	String closed_date;
	String title;
	String tags;
	long answer_count;
	long comment_count;
	long favorite_count;
	
	//  <row Id="20" PostTypeId="1" AcceptedAnswerId="22" CreationDate="2014-09-24T13:40:12.750" Score="9" ViewCount="174" Body="&lt;p&gt;" OwnerUserId="12" LastEditorUserId="105" LastEditDate="2014-09-27T16:10:05.933" LastActivityDate="2015-05-15T15:31:15.983" Title="How should we name our chat room?" 
	//		Tags="&lt;discussion&gt;&lt;chat&gt;" AnswerCount="5" CommentCount="1" />
	//  <row Id="21" PostTypeId="2" ParentId="20" CreationDate="2014-09-24T14:06:12.703" Score="7" Body="&lt;p&gt;&lt;code&gt;M-x chat&lt;/code&gt; would be my suggestion. But something less obvious may be better...&lt;/p&gt;&#xA;" OwnerUserId="15" LastActivityDate="2014-09-24T14:06:12.703" CommentCount="1" />

	public static PostObject parseElement(Element e) {
		PostObject p = new PostObject();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setPost_type_id(e.getAttribute("PostTypeId"));
		p.setParent_id(e.getAttribute("ParentId"));
		p.setAccepted_answer_id(e.hasAttribute("AcceptedAnswerId") ? e.getAttribute("AcceptedAnswerId") : null);
		p.setCreation_date(ParseUtil.convertDate(e.getAttribute("CreationDate")));
		p.setAnswer_count(ParseUtil.parseLong(e, "AnswerCount"));
		p.setBody(e.getAttribute("Body"));
		p.setClosed_date(e.getAttribute("ClosedDate"));
		p.setComment_count(ParseUtil.parseLong(e, "CommentCount"));
		p.setFavorite_count(ParseUtil.parseLong(e, "FavoriteCount"));
		p.setOwner_user_id(e.getAttribute("OwnerUserId"));
		p.setScore(ParseUtil.parseLong(e, "Score"));
		p.setTags(parseTag(e.getAttribute("Tags")));
		p.setTitle(e.getAttribute("Title"));
		p.setView_count(ParseUtil.parseLong(e, "ViewCount"));
		return p;
	}
	
	private static String parseTag(String tag) {
		if (tag != null || !tag.isEmpty()) {
			tag = tag.replaceAll("><", ",");
			tag = tag.replace("<", "");
			tag = tag.replace(">", "");
		}
		return tag;
	}
	
	public static List<PostObject> flattenTags(PostObject postInput) {
		List<PostObject> flattenPosts = new ArrayList<PostObject>();
		if (postInput == null || postInput.getId().isEmpty())
			return flattenPosts;
		
		String tags = postInput.getTags();

		if (tags != null && !tags.isEmpty()) {
			for (String tag: tags.split(",")) {
				PostObject newPost = new PostObject();
				
				newPost.setId(postInput.getId());
				newPost.setPost_type_id(postInput.getPost_type_id());
				newPost.setParent_id(postInput.getParent_id());
				newPost.setAccepted_answer_id(postInput.getAccepted_answer_id());
				newPost.setCreation_date(postInput.getCreation_date());
				newPost.setAnswer_count(postInput.getAnswer_count());
				newPost.setBody(postInput.getBody());
				newPost.setClosed_date(postInput.getClosed_date());
				newPost.setComment_count(postInput.getComment_count());
				newPost.setFavorite_count(postInput.getFavorite_count());
				newPost.setOwner_user_id(postInput.getOwner_user_id());
				newPost.setScore(postInput.getScore());
				newPost.setTags(tag);
				newPost.setTitle(postInput.getTitle());
				newPost.setView_count(postInput.getView_count());
				
				flattenPosts.add(newPost);
			}
		} else {
			flattenPosts.add(postInput);
		}
		
		return flattenPosts;
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

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public long getView_count() {
		return view_count;
	}

	public void setView_count(long view_count) {
		this.view_count = view_count;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getOwner_user_id() {
		return owner_user_id;
	}

	public void setOwner_user_id(String owner_user_id) {
		this.owner_user_id = owner_user_id;
	}

	public String getClosed_date() {
		return closed_date;
	}

	public void setClosed_date(String closed_date) {
		this.closed_date = closed_date;
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

	public long getAnswer_count() {
		return answer_count;
	}

	public void setAnswer_count(long answer_count) {
		this.answer_count = answer_count;
	}

	public long getComment_count() {
		return comment_count;
	}

	public void setComment_count(long comment_count) {
		this.comment_count = comment_count;
	}

	public long getFavorite_count() {
		return favorite_count;
	}

	public void setFavorite_count(long favorite_count) {
		this.favorite_count = favorite_count;
	}

}
