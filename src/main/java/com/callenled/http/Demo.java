package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        String result = HttpUtil.builder()
                .doGet("https://api.mch.weixin.qq.com/secapi/pay/refund")
                .toJson();
        System.out.println(result);
    }
}
