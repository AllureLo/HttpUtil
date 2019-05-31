package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {

        while (true) {
            String result = HttpUtil.builder()
                    .doGet("https://www.baidu.com")
                    .toJson();
            System.out.println(result);
        }
    }
}
