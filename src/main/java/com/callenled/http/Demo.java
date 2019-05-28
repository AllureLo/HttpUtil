package com.callenled.http;

import com.callenled.util.HttpUtil;

/**
 * @Author: Callenld
 * @Date: 19-3-25
 */
public class Demo {
    public static void main(String[] args) {
        ResponseObjcet<Data> responseObjcet = HttpUtil.builder()
                .addParams("from", 2)
                .doGet("https://app.teambook.cc/lindoor/common/question")
                .toResponseObject(ResponseObjcet.class, Data.class);
        System.out.println(responseObjcet.toString());
    }
}
