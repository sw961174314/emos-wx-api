package com.sw961174314.emos.wx.service;

import com.sw961174314.emos.wx.db.pojo.TbDept;

import java.util.List;

public interface DeptService {

    // 查询所有部门
    public List<TbDept> searchAllDept();

    // 新增部门
    public int insertDept(String deptName);

    // 删除部门
    public void deleteDeptById(int id);

    // 更改部门
    public void updateDeptById(TbDept entity);
}
