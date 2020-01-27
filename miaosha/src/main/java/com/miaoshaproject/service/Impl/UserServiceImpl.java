package com.miaoshaproject.service.Impl;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @Author: crowsjian
 * @Date: 2019/11/24 8:36
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        // 通过userDOMapper获取到对应的用户的dataobject
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if(userDO == null){
            return null;
        }
        // 通过用户id获取对应的用户加密密码信息
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        return convertFromDO(userDO, userPasswordDO);
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel)redisTemplate.opsForValue().get("user_validate_"+id);
        if(userModel == null ){
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_validate_"+id,userModel);
            redisTemplate.expire("user_validate_"+id, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    // 登录
    @Override
    public UserModel login(String telphone, String encrptPassword) throws BusinessException {
        // 通过 用户手机而得到用户信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO == null){
            throw new BusinessException(EMBusinessError.USER_LOGIN_ERROR);
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDO(userDO, userPasswordDO);
        // 比对信息内密码是否和传输的密码相匹配
        if(!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())){
            throw new BusinessException(EMBusinessError.USER_LOGIN_ERROR);
        }
        return userModel;
    }

    /**
     * 注册
     * @param userModel
     */
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        // 验证参数
        if(userModel == null){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR);
        }
//        if(StringUtils.isEmpty(userModel.getName())
//            ||userModel.getGender()==null
//            ||userModel.getAge()==null
//            ||StringUtils.isEmpty(userModel.getTelphone())){
//            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR);
//        }
        ValidationResult validationResult = validator.validate(userModel);
        if(validationResult.isHasErrors()){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, validationResult.getErrorMsg());
        }
        // 实现model=>dataobkect
        UserDO userDO = convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        }catch (DuplicateKeyException e){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, "手机号不能重复注册");
        }
        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
        return;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserPasswordDO result = new UserPasswordDO();
        result.setEncrptPassword(userModel.getEncrptPassword());
        result.setUserId(userModel.getId());
        return result;
    }

    private UserDO convertFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserDO result = new UserDO();
        BeanUtils.copyProperties(userModel, result);
        return result;
    }

    private UserModel convertFromDO(UserDO userDO, UserPasswordDO userPasswordDO){
        //
        if(userDO == null){
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        if(userPasswordDO != null){
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }
}
