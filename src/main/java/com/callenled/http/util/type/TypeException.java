package com.callenled.http.util.type;

/**
 * @Author: Callenld
 * @Date: 19-5-28
 */
public class TypeException extends RuntimeException {
    private static final long serialVersionUID = 4951015210045552168L;

    public TypeException() {
    }

    public TypeException(String message) {
        super(message);
    }

    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeException(Throwable cause) {
        super(cause);
    }

    public TypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
