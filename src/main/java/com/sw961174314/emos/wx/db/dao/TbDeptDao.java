package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface TbDeptDao {

    // 查询部门成员
    public ArrayList<HashMap> searchDeptMembers(String keyword);

    // 查询所有部门
    public List<TbDept> searchAllDept();

    // 新增部门
    public int insertDept(String deptName);

    // 删除部门
    public int deleteDeptById(int id);

    // 更改部门
    public int updateDeptById(TbDept entity);
}
