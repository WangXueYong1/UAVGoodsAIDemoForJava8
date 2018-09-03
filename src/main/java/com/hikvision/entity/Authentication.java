package com.hikvision.entity;

public class Authentication {
	String token;
	String notice;
	String result;

	public String getToken() {
		return token;
	}

	public String getNotice() {
		return notice;
	}

	public String getResult() {
		return result;
	}

	public String getRoundId() {
		return roundId;
	}

	public String getYourId() {
		return yourId;
	}

	String roundId;
	String yourId;
}
