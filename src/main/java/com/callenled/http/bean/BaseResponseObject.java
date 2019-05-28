package com.callenled.http.bean;

import java.io.Serializable;

/**
 * @Author: Callenld
 * @Date: 19-5-28
 */
public abstract class BaseResponseObject<T> implements Serializable {

    private static final long serialVersionUID = -4112426223810612178L;

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "BaseResponseObject{" +
                "data=" + data +
                '}';
    }
}
