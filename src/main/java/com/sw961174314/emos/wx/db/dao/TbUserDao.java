package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

@Mapper
public interface TbUserDao {
    // 判定系统是否已经绑定超级管理员
    public boolean haveRootUser();

    // 保存用户记录的代码
    public int insert(HashMap param);

    // 查询用户ID的代码
    public Integer searchIdByOpenId(String openId);

    // 查询用户的权限列表
    public Set<String> searchUserPermissions(int userId);

    // 查询用户ID
    public TbUser searchById(int userId);

    // 查询员工的姓名和部门名称
    public HashMap searchNameAndDept(int userId);

    // 查询员工的入职日期
    public String searchUserHiredate(int userId);

    // 查询用户概要信息
    public HashMap searchUserSummary(int userId);

    // 查询部门成员
    public ArrayList<HashMap> searchUserGroupByDept(String keyword);

    // 查询会议成员信息
    public ArrayList<HashMap> searchMembers(List param);

    // 查询用户的概要信息
    public HashMap searchUserInfo(int userId);

    // 查询部门经理
    public int searchDeptManagerId(int id);

    // 查询总经理
    public int searchGmId();

    // 查询审批人的头像和姓名
    public List<HashMap> selectUserPhotoAndName(List param);

    // 查询对应部门是否有成员(没有则可以删除该部门)
    public long searchUserCountInDept(int deptId);

    // 根据绑定邮箱查找userId
    public int searchUserIdByEmail(String email);

    // 更新用户微信信息
    public int activeUserAccount(HashMap param);

    // 更新员工信息
    public int updateUserInfo(HashMap param);

    // 删除员工信息
    public int deleteUserById(int id);

    // 查询员工列表(通讯录)
    public ArrayList<HashMap> searchUserContactList();
}
