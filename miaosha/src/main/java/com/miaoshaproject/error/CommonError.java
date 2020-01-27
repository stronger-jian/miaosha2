package com.miaoshaproject.error;

/**
 * @Author: crowsjian
 * @Date: 2019/11/24 12:34
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
