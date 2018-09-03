/**
  * Copyright 2018 bejson.com 
  */
package com.hikvision.entity;

public class UAV_we {
	public Integer no;
	public String type;
	public Integer x;
	public Integer y;
	public Integer z;
	public Integer goods_no;
	public Integer status;
	public Integer remain_electricity;

	public Integer getRemain_electricity() {
		return remain_electricity;
	}

	public void setRemain_electricity(Integer remain_electricity) {
		this.remain_electricity = remain_electricity;
	}

	public Integer getNo() {
		return no;
	}

	public void setNo(Integer no) {
		this.no = no;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getZ() {
		return z;
	}

	public void setZ(Integer z) {
		this.z = z;
	}

	public Integer getGoods_no() {
		return goods_no;
	}

	public void setGoods_no(Integer goods_no) {
		this.goods_no = goods_no;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public UAV_we() {

	}

	public UAV_we(Integer no, String type, Integer x, Integer y, Integer z, Integer goods_no, Integer status, Integer remain_electricity) {
		super();
		this.no = no;
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.goods_no = goods_no;
		this.status = status;
		this.remain_electricity = remain_electricity;
	}

	@Override
	public String toString() {
		return "UAV_we [no=" + no + ", type=" + type + ", x=" + x + ", y=" + y + ", z=" + z + ", goods_no=" + goods_no
				+ ", status=" + status + ", remain_electricity=" + remain_electricity + "]";
	}

	

}