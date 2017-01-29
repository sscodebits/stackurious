package com.shikha.stackoverflow.common;

import org.w3c.dom.Element;

import com.shikha.stackoverflow.util.ParseUtil;

public class PostObject {
	String id;
	String postId;
	String parentId;
	String acceptedAnswerId;
	String creationDate;
	long score;
	long viewCount;
	String body;
	String ownerUserId;
	String closedDate;
	String title;
	String tags;
	long answerCount;
	long commentCount;
	long favoriteCount;
	
	public static PostObject parseElement(Element e) {
		PostObject p = new PostObject();
		if (e == null || e.getAttribute("Id").isEmpty()) {
			p.setId("");
			return p;
		}
		
		p.setId(e.getAttribute("Id"));
		p.setPostId(e.getAttribute("PostId"));
		p.setParentId(e.getAttribute("ParentId"));
		p.setAcceptedAnswerId(e.getAttribute("AcceptedAnswerId"));
		p.setAnswerCount(ParseUtil.parseLong(e, "AnswerCount"));
		p.setBody(e.getAttribute("Body"));
		p.setClosedDate(e.getAttribute("ClosedDate"));
		p.setCommentCount(ParseUtil.parseLong(e, "CommentCount"));
		p.setFavoriteCount(ParseUtil.parseLong(e, "FavoriteCount"));
		p.setOwnerUserId(e.getAttribute("OwnerUserId"));
		p.setScore(ParseUtil.parseLong(e, "Score"));
		p.setTags(e.getAttribute("Tags"));
		p.setTitle(e.getAttribute("Title"));
		p.setViewCount(ParseUtil.parseLong(e, "ViewCount"));
		return p;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPostId() {
		return postId;
	}

	public void setPostId(String postId) {
		this.postId = postId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getAcceptedAnswerId() {
		return acceptedAnswerId;
	}

	public void setAcceptedAnswerId(String acceptedAnswerId) {
		this.acceptedAnswerId = acceptedAnswerId;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public long getViewCount() {
		return viewCount;
	}

	public void setViewCount(long viewCount) {
		this.viewCount = viewCount;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public String getClosedDate() {
		return closedDate;
	}

	public void setClosedDate(String closedDate) {
		this.closedDate = closedDate;
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

	public long getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(long answerCount) {
		this.answerCount = answerCount;
	}

	public long getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(long commentCount) {
		this.commentCount = commentCount;
	}

	public long getFavoriteCount() {
		return favoriteCount;
	}

	public void setFavoriteCount(long favoriteCount) {
		this.favoriteCount = favoriteCount;
	}

}
