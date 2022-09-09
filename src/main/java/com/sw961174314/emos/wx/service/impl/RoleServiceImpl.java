package com.sw961174314.emos.wx.service.impl;

import cn.hutool.json.JSONObject;
import com.sw961174314.emos.wx.db.dao.TbRoleDao;
import com.sw961174314.emos.wx.db.pojo.TbRole;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Autowired
    private TbRoleDao roleDao;

    /**
     * 查询角色权限列表
     * @param id
     * @return
     */
    @Override
    public ArrayList<HashMap> searchRoleOwnPermission(int id) {
        ArrayList<HashMap> list = roleDao.searchRoleOwnPermission(id);
        list = handleData(list);
        return list;
    }

    /**
     * 将查询结果按照模块名称分组
     * @param list
     * @return
     */
    private ArrayList<HashMap> handleData(ArrayList<HashMap> list) {
        ArrayList permsList = new ArrayList();
        ArrayList actionList = new ArrayList();
        HashSet set = new HashSet();
        HashMap data = new HashMap();

        for (HashMap map : list) {
            long permissionId = (Long) map.get("id");
            String moduleName = (String) map.get("moduleName");
            String actionName = (String) map.get("actionName");
            String selected = map.get("selected").toString();

            if (set.contains(moduleName)) {
                JSONObject json = new JSONObject();
                json.set("id", permissionId);
                json.set("actionName", actionName);
                json.set("selected", selected.equals("1") ? true : false);
                actionList.add(json);
            } else {
                set.add(moduleName);
                data = new HashMap();
                data.put("moduleName", moduleName);
                actionList = new ArrayList();
                JSONObject json = new JSONObject();
                json.set("id", permissionId);
                json.set("actionName", actionName);
                json.set("selected", selected.equals("1") ? true : false);
                actionList.add(json);
                data.put("action", actionList);
                permsList.add(data);
            }

        }
        return permsList;
    }

    /**
     * 查询所有权限
     * @return
     */
    @Override
    public ArrayList<HashMap> searchAllPermission() {
        ArrayList<HashMap> list = roleDao.searchAllPermission();
        list = handleData(list);
        return list;
    }

    /**
     * 新建角色
     * @param role
     */
    @Override
    public void insertRole(TbRole role) {
        int row = roleDao.insertRole(role);
        if (row != 1) {
            throw new EmosException("添加角色失败");
        }
    }

    /**
     * 修改已有角色
     * @param role
     */
    @Override
    public void updateRolePermissions(TbRole role) {
        int row = roleDao.updateRolePermissions(role);
        if (row != 1) {
            throw new EmosException("修改角色失败");
        }
    }

    /**
     * 查询所有角色数据
     * @return
     */
    @Override
    public List<TbRole> searchAllRole() {
        List<TbRole> list = roleDao.searchAllRole();
        return list;
    }

    /**
     * 删除角色
     * @param id
     */
    @Override
    public void deleteRoleById(int id) {
        long count = roleDao.searchRoleUsersCount(id);
        if (count > 0) {
            throw new EmosException("该角色关联用户，无法删除");
        }
        int row = roleDao.deleteRoleById(id);
        if (row != 1) {
            throw new EmosException("角色删除失败");
        }
    }
}
