package com.sw961174314.emos.wx.service.impl;

import com.sw961174314.emos.wx.db.dao.TbDeptDao;
import com.sw961174314.emos.wx.db.dao.TbUserDao;
import com.sw961174314.emos.wx.db.pojo.TbDept;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DeptServiceImpl implements DeptService {

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbDeptDao deptDao;

    /**
     * 查询所有部门
     * @return
     */
    @Override
    public List<TbDept> searchAllDept() {
        List<TbDept> list = deptDao.searchAllDept();
        return list;
    }

    /**
     * 新增部门
     * @param deptName
     * @return
     */
    @Override
    public int insertDept(String deptName) {
        int row = deptDao.insertDept(deptName);
        if (row != 1) {
            throw new EmosException("部门添加失败");
        }
        return row;
    }

    /**
     * 删除部门
     * @param id
     */
    @Override
    public void deleteDeptById(int id) {
        // 查询部门是否有成员
        long count = userDao.searchUserCountInDept(id);
        if (count > 0) {
            throw new EmosException("部门中有员工，无法删除部门");
        } else {
            int row = deptDao.deleteDeptById(id);
            if (row != 1) {
                throw new EmosException("部门删除失败");
            }
        }
    }

    /**
     * 更改部门
     * @param entity
     */
    @Override
    public void updateDeptById(TbDept entity) {
        int row = deptDao.updateDeptById(entity);
        if (row != 1) {
            throw new EmosException("部门更新失败");
        }
    }
}
