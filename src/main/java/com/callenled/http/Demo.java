package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        String result = HttpUtil.config(100, 100)
                .setTimeout(20000, 20000)
                .setSSLContext("", "")
                .setOnRetryTimes()
                .builder()
                .addParams("https", "xxxxxxxxx")
                .doGet("wwwwwwwwwwww")
                .toJson();
        System.out.println(result);
    }
}
