package org.elastos.util;

import org.elastos.constant.RetCode;

public class RetResult<T> {
    private Long code;
    private String msg;
    private T data;

    public Long getCode() {
        return code;
    }

    public RetResult<T>setCode(Long code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public RetResult<T> setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public RetResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    public static <T> RetResult<T> retOk(T data) {
        return new RetResult<T>().setCode(RetCode.SUCC).setData(data);
    }

    public static <T> RetResult<T> retErr(long code , String msg) {
        return new RetResult<T>().setCode(code).setMsg(msg);
    }

}
