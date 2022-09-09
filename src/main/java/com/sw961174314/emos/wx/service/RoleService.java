package com.sw961174314.emos.wx.service;

import com.sw961174314.emos.wx.db.pojo.TbRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface RoleService {

    // 查询角色权限列表
    public ArrayList<HashMap> searchRoleOwnPermission(int id);

    // 查询所有权限
    public ArrayList<HashMap> searchAllPermission();

    // 新建角色
    public void insertRole(TbRole role);

    // 修改已有角色
    public void updateRolePermissions(TbRole role);

    // 查询所有角色数据
    public List<TbRole> searchAllRole();

    // 删除角色
    public void deleteRoleById(int id);
}
