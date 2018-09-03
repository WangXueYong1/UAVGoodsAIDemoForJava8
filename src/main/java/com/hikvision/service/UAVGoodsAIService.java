package com.hikvision.service;

import com.alibaba.fastjson.JSONObject;
import com.hikvision.entity.JSONDemo;

/**
 * Created by lifubang on 2018/4/27.
 */
public class UAVGoodsAIService {
    public void hello() {
        String jsonString = "{\n" +
                "  \"name\": \"UAVGoodsAI\",\n" +
                "  \"age\": 1\n" +
                "}";
        JSONDemo jsonDemo = JSONObject.parseObject(jsonString, JSONDemo.class);
        System.out.println(String.format("Hello %s, I'm %d year(s) old.", jsonDemo.name, jsonDemo.age));
    }
}
