/**
  * Copyright 2018 bejson.com 
  */
package com.hikvision.entity;

import java.util.ArrayList;

/**
 * Auto-generated: 2018-05-14 14:15:36
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class StepObj {

	public String token;
	public String notice;
	public int match_status;
	public int time;
	public ArrayList<UAV_we> UAV_we;
	public int we_value;
	public ArrayList<UAV_enemy> UAV_enemy;
	public int enemy_value;
	public ArrayList<Goods> goods;

	public void setToken(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public String getNotice() {
		return notice;
	}

	public void setMatch_status(int match_status) {
		this.match_status = match_status;
	}

	public int getMatch_status() {
		return match_status;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public int getTime() {
		return time;
	}

	public void setUAV_we(ArrayList<UAV_we> UAV_we) {
		this.UAV_we = UAV_we;
	}

	public ArrayList<UAV_we> getUAV_we() {
		return UAV_we;
	}

	public void setWe_value(int we_value) {
		this.we_value = we_value;
	}

	public int getWe_value() {
		return we_value;
	}

	public void setUAV_enemy(ArrayList<UAV_enemy> UAV_enemy) {
		this.UAV_enemy = UAV_enemy;
	}

	public ArrayList<UAV_enemy> getUAV_enemy() {
		return UAV_enemy;
	}

	public void setEnemy_value(int enemy_value) {
		this.enemy_value = enemy_value;
	}

	public int getEnemy_value() {
		return enemy_value;
	}

	public void setGoods(ArrayList<Goods> goods) {
		this.goods = goods;
	}

	public ArrayList<Goods> getGoods() {
		return goods;
	}

}