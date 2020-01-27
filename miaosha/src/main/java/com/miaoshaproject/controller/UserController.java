package com.miaoshaproject.controller;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobjcet.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.apache.tomcat.util.codec.binary.Base64;
import sun.security.provider.MD5;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: crowsjian
 * @Date: 2019/11/24 8:28
 */
@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials="true", allowedHeaders="*")
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    //redis 操作类
    @Autowired
    private RedisTemplate redisTemplate;

    // 用户登录接口
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone")String telphone,
                                  @RequestParam(name = "password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //校验参数
        if(org.apache.commons.lang3.StringUtils.isEmpty(telphone)||
            StringUtils.isEmpty(password)){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR);
        }
        // 用户登录服务，用来校验用户登录是否合法
        UserModel userModel = userService.login(telphone, this.EncodeByMD5(password));

        // 修改
        // 若用户登陆验证成功后，将对应的登录信息和登陆凭证一起存入redis中
        // 生成登陆凭证 token，UUID
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-","");
        // 建立token和用户登录态之间的联系
        redisTemplate.opsForValue().set(uuidToken, userModel);
        // 设置超时时间
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

        // 将登陆凭证加入到用户登录成功的session内
//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        // 下发  token返回uuid给客户端
        return CommonReturnType.create(uuidToken);
    }

    // 用户注册接口
    @RequestMapping(value = "/register", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone")String telphone,
                                     @RequestParam(name = "name")String name,
                                     @RequestParam(name = "gender")Integer gender,
                                     @RequestParam(name = "age")Integer age,
                                     @RequestParam(name = "password")String password,
                                     @RequestParam(name = "otpCode")String otpCode) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        // 验证手机号和对应otpCode相符
        String inSessionOtpCode =  (String) httpServletRequest.getSession().getAttribute(telphone);
        if(!StringUtils.equals(inSessionOtpCode, otpCode)){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR,  "短信验证码不正确");
        }
        // 用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setTelphone(telphone);
        userModel.setName(name);
        userModel.setGender(gender.byteValue());
        userModel.setAge(age);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMD5(password));
        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    public String EncodeByMD5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // 确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
       /* BASE64Encoder base64Encoder = new BASE64Encoder();
        // 加密字符串
        String newstr = base64Encoder.encode(md5.digest(str.getBytes("UTF-8")));*/
        String newStr = Base64.encodeBase64String(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    /**
     * 获取短信验证
     * @param telphone
     * @return
     */
    @RequestMapping(value = "/getOtp", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone){
        // 按规则生成 otp 验证码
        Random random = new Random();
        int intOtp = random.nextInt(99999);
        intOtp += 10000;
        String otpStr = String.valueOf(intOtp);
        // 将otp验证码与用户手机号对应，通过httpsession 绑定
        httpServletRequest.getSession().setAttribute(telphone, otpStr);
        // 将otp发送给用户
        System.out.println("telphone:"+telphone+"&otpCode:"+otpStr);
        return CommonReturnType.create(null);
    }

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id")Integer id) throws BusinessException {
        // 调用 service 服务层获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);
        if(userModel == null){
            userModel.setEncrptPassword("111");
            //throw new BusinessException(EMBusinessError.USER_NOT_EXIST);
        }
        // 将核心领域模型用户对象转换为供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);
        // 返回通用对象
        return CommonReturnType.create(userVO);
    }

    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }

}
