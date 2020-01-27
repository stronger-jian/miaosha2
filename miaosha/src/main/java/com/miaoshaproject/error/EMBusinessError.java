package com.miaoshaproject.error;

/**
 * @Author: crowsjian
 * @Date: 2019/11/24 12:36
 */
public enum EMBusinessError implements CommonError{
    // 通用错误类型10001
    PARAMATER_VALIDATION_ERROR(10001, "参数错误"),
    UNKNOW_ERROR(10002, "未知错误"),
    // 定义以20000开头为用户相关错误定义
    USER_NOT_EXIST(20001, "用户不存在"),
    USER_LOGIN_ERROR(20002, "用户登录手机号或密码不正确！"),
    USER_NOT_LOGIN(20003, "用户还未登录"),
    // 定义以20000开头为交易信息错误
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    MQ_SEND_FAIL(30002, "库存异步消息失败"),
    RATE_LIMIT(30003, "活动火爆，请稍后再试"),
    ;

    private Integer errCode;
    private String errMsg;

    private EMBusinessError(Integer errCode, String errMsg){
        this.errCode=errCode;
        this.errMsg=errMsg;
    }


    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
