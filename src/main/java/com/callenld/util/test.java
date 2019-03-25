package com.callenld.util;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class test {
    public static void main(String[] args) {
        String json = HttpUtil.builder()
                .addParams("appid", "appId")
                .addParams("secret", "secret")
                .addParams("js_code", "code")
                .addParams("grant_type", "authorization_code")
                .doGet("https://api.weixin.qq.com/sns/jscode2session")
                .toJson();
    }
}
