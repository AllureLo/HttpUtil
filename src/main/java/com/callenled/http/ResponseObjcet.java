package com.callenled.http;

import com.callenled.http.bean.BaseResponseObject;

/**
 * @Author: Callenld
 * @Date: 19-5-28
 */
public class ResponseObjcet<T> extends BaseResponseObject<T> {

    private static final long serialVersionUID = -2013770706048328120L;

    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "ResponseObjcet{" +
                "code=" + code +
                '}';
    }
}
