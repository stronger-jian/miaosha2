package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: crowsjian
 * @Date: 2019/12/8 10:21
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonReturnType doError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    Exception ex){
        ex.printStackTrace();
        Map<String, Object> responseData = new HashMap<>();
        if(ex instanceof BusinessException){
            BusinessException businessException = (BusinessException)ex;
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        }else if(ex instanceof ServletRequestBindingException){
            responseData.put("errCode", EMBusinessError.UNKNOW_ERROR.getErrCode());
            responseData.put("errMsg", "url绑定路由有问题");
        }else if(ex instanceof NoHandlerFoundException){
            responseData.put("errCode", EMBusinessError.UNKNOW_ERROR.getErrCode());
            responseData.put("errMsg", "没有找到对应的访问路径");
        }else{
            responseData.put("errCode", EMBusinessError.UNKNOW_ERROR.getErrCode());
            responseData.put("errMsg", EMBusinessError.UNKNOW_ERROR.getErrMsg());
        }
        return CommonReturnType.create(responseData, "fail");
    }
}
