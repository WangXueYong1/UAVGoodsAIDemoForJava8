/**
  * Copyright 2018 bejson.com 
  */
package com.hikvision.entity;
public class Goods {
	public int no;
	public int start_x;
	public int start_y;
	public int end_x;
	public int end_y;
	public int weight;
	public int value;
	public int start_time;
	public int remain_time;
	public int left_time;
	public int status;

	public int getLeft_time() {
		return left_time;
	}

	public void setLeft_time(int left_time) {
		this.left_time = left_time;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public int getStart_x() {
		return start_x;
	}

	public void setStart_x(int start_x) {
		this.start_x = start_x;
	}

	public int getStart_y() {
		return start_y;
	}

	public void setStart_y(int start_y) {
		this.start_y = start_y;
	}

	public int getEnd_x() {
		return end_x;
	}

	public void setEnd_x(int end_x) {
		this.end_x = end_x;
	}

	public int getEnd_y() {
		return end_y;
	}

	public void setEnd_y(int end_y) {
		this.end_y = end_y;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getStart_time() {
		return start_time;
	}

	public void setStart_time(int start_time) {
		this.start_time = start_time;
	}

	public int getRemain_time() {
		return left_time;
	}

	public void setRemain_time(int remain_time) {
		this.left_time = remain_time;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}


	@Override
	public String toString() {
		return "Goods [no=" + no + ", start_x=" + start_x + ", start_y=" + start_y + ", end_x=" + end_x + ", end_y="
				+ end_y + ", weight=" + weight + ", value=" + value + ", start_time=" + start_time + ", remain_time="
				+ left_time + ", status=" + status + "]";
	}

	public Goods() {

	}

	public Goods(int no, int start_x, int start_y, int end_x, int end_y, int weight, int value, int start_time,
			int remain_time, int status) {
		super();
		this.no = no;
		this.start_x = start_x;
		this.start_y = start_y;
		this.end_x = end_x;
		this.end_y = end_y;
		this.weight = weight;
		this.value = value;
		this.start_time = start_time;
		this.left_time = remain_time;
		this.status = status;
	}

}