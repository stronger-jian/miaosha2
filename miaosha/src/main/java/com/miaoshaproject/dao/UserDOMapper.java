package com.miaoshaproject.dao;

import com.miaoshaproject.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Component
public interface UserDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    int insert(UserDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    int insertSelective(UserDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    UserDO selectByPrimaryKey(Integer id);

    //通过手机号得到用户信息
    UserDO selectByTelphone(String telphone);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    int updateByPrimaryKeySelective(UserDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table user_info
     *
     * @mbg.generated Sat Nov 23 23:25:31 CST 2019
     */
    int updateByPrimaryKey(UserDO record);
}