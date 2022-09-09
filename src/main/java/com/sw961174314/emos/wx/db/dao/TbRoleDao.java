package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface TbRoleDao {

    // 查询角色权限列表
    public ArrayList<HashMap> searchRoleOwnPermission(int id);

    // 查询所有权限
    public ArrayList<HashMap> searchAllPermission();

    // 新建角色
    public int insertRole(TbRole role);

    // 修改已有角色
    public int updateRolePermissions(TbRole role);

    // 查询所有角色
    public List<TbRole> searchAllRole();

    // 查询用户的角色数目
    public long searchRoleUsersCount(int id);

    // 删除角色
    public int deleteRoleById(int id);
}
