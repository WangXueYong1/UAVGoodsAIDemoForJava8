/**
  * Copyright 2018 bejson.com 
  */
package com.hikvision.entity;
import java.util.List;

/**
 * Auto-generated: 2018-05-14 15:17:51
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class SendObj {

	public String token;
	public String action;
	public List<UAV_info> UAV_info;
	public List<Purchase_UAV> purchase_UAV;
    public void setToken(String token) {
         this.token = token;
     }
     public String getToken() {
         return token;
     }

    public void setAction(String action) {
         this.action = action;
     }
     public String getAction() {
         return action;
     }

    public void setUAV_info(List<UAV_info> UAV_info) {
         this.UAV_info = UAV_info;
     }
     public List<UAV_info> getUAV_info() {
         return UAV_info;
     }

    public void setPurchase_UAV(List<Purchase_UAV> purchase_UAV) {
         this.purchase_UAV = purchase_UAV;
     }
     public List<Purchase_UAV> getPurchase_UAV() {
         return purchase_UAV;
     }

}