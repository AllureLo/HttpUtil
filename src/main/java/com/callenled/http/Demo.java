package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        String result = HttpUtil.builder()
                .addParams("param", "param")
                .signWithMD5("0000000000000000000000")
                .doPost("http://xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .toJson();
        System.out.println(result);
    }
}
