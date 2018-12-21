package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.UserAddress;
import com.atguigu.gmall0715.bean.UserInfo;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

import java.util.List;

public interface UserinfoService {
    List<UserInfo> fandAll();
    //根据用户ID查询用户地址
    List<UserAddress> fandUserAddressByUserId(String userId);

    //登录的方法
    UserInfo login(UserInfo userInfo);

    //根据用户Id验证数据
    UserInfo verify(String userId);
}
