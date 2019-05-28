package com.callenled.http;

import com.callenled.http.bean.BaseResponseObject;
import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        String result = HttpUtil.builder()
                .addParams("appid", "appId")
                .addParams("secret", "secret")
                .addParams("js_code", "code")
                .addParams("grant_type", "authorization_code")
                .doGet("https://api.weixin.qq.com/sns/jscode2session")
                .toJson();
        System.out.println(result);

        BaseResponseObject<Data> responseObjcet = HttpUtil.builder()
                .addParams("appid", "appId")
                .addParams("secret", "secret")
                .addParams("js_code", "code")
                .addParams("grant_type", "authorization_code")
                .doGet("https://api.weixin.qq.com/sns/jscode2session")
                .toResponseObject(Data.class);

    }
}
