package com.shikha.stackoverflow.common;

import java.io.Serializable;

public class JavaCard implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String suit;
	int num;
	
	public String getSuit() {
		return suit;
	}
	public void setSuit(String suit) {
		this.suit = suit;
	}
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	
	
}
