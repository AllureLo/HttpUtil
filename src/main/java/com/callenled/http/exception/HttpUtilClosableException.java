package com.callenled.http.exception;

/**
 * @Author: Callenld
 * @Date: 19-5-29
 */
public class HttpUtilClosableException extends Exception {
    private static final long serialVersionUID = -9077671250140340527L;

    /**
     * Constructs a HttpUtilClosableException with no detail message.
     */
    public HttpUtilClosableException() {
        super();
    }

    /**
     * Constructs a HttpUtilClosableException with the specified detail
     * message.
     * A detail message is a String that describes this particular
     * exception.
     *
     * @param msg the detail message.
     */
    public HttpUtilClosableException(String msg) {
        super(msg);
    }

    /**
     * Creates a {@code HttpUtilClosableException} with the specified
     * detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     * @since 1.5
     */
    public HttpUtilClosableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@code HttpUtilClosableException} with the specified cause
     * and a detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of
     * {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A {@code null} value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     * @since 1.5
     */
    public HttpUtilClosableException(Throwable cause) {
        super(cause);
    }
}
