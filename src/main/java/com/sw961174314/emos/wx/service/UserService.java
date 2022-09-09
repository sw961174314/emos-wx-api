package com.sw961174314.emos.wx.service;

import cn.hutool.json.JSONObject;
import com.sw961174314.emos.wx.db.pojo.TbUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface UserService {

    // 注册新用户
    int registerUser(String registerCode, String code, String nickname, String photo);

    // 查询用户的权限列表
    Set<String> searchUserPermissions(int userId);

    // 用户登录
    Integer login(String code);

    // Shiro认证功能
    TbUser searchById(int userId);

    // 查询员工的入职日期
    String searchUserHiredate(int userId);

    // 查询用户的概要信息
    HashMap searchUserSummary(int userId);

    // 查询部门成员
    ArrayList<HashMap> searchUserGroupByDept(String keyword);

    // 查询会议成员信息
    ArrayList<HashMap> searchMembers(List param);

    // 查询审批人的头像和姓名
    List<HashMap> selectUserPhotoAndName(List param);

    // 添加普通员工
    public void insertUser(HashMap param);

    // 查询员工基本信息
    public HashMap searchUserInfo(int userId);

    // 更新员工信息
    public int updateUserInfo(HashMap param);

    // 删除员工信息
    public void deleteUserById(int id);

    // 查询员工列表(通讯录)
    public JSONObject searchUserContactList();
}
