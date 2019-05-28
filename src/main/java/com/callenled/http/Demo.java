package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        String result = HttpUtil.builder()
                .addParams("random", "wefwefdsfsdfsfasdf")
                .addParams("company", "盂县程子岩玉米种植专业合作社")
                .signWithMD5("sdifjwoiejfowjdsfjsf")
                .doPost("http://www.912688.com/spider/baiduWenku")
                .toJson();
        System.out.println(result);
    }
}
