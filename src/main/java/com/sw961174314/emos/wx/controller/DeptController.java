package com.sw961174314.emos.wx.controller;

import com.sw961174314.emos.wx.common.util.R;
import com.sw961174314.emos.wx.controller.form.DeleteDeptByIdForm;
import com.sw961174314.emos.wx.controller.form.InsertDeptForm;
import com.sw961174314.emos.wx.controller.form.UpdateDeptByIdForm;
import com.sw961174314.emos.wx.db.pojo.TbDept;
import com.sw961174314.emos.wx.service.DeptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/dept")
@Slf4j
@Api("部门模块网络接口")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/searchAllDept")
    @ApiOperation("查询所有部门数据")
    public R searchAllDept() {
        List<TbDept> list = deptService.searchAllDept();
        return R.ok().put("result", list);
    }

    @PostMapping("/insertDept")
    @ApiOperation("新增部门")
//    @RequiresPermissions(value = {"ROOT","DEPT:INSERT"},logical = Logical.OR)
    public R insertDept(@Valid @RequestBody InsertDeptForm form) {
        deptService.insertDept(form.getDeptName());
        return R.ok().put("result", "success");
    }

    @PostMapping("/deleteDeptById")
    @ApiOperation("删除部门")
//    @RequiresPermissions(value = {"ROOT","DEPT:DELETE"},logical = Logical.OR)
    public R deleteDeptById(@Valid @RequestBody DeleteDeptByIdForm form) {
        deptService.deleteDeptById(form.getId());
        return R.ok().put("result", "success");
    }

    @PostMapping("/updateDeptById")
    @ApiOperation("更改部门")
//    @RequiresPermissions(value = {"ROOT","DEPT:UPDATE"},logical = Logical.OR)
    public R updateDeptById(@Valid @RequestBody UpdateDeptByIdForm form) {
        TbDept entity = new TbDept();
        entity.setId(form.getId());
        entity.setDeptName(form.getDeptName());
        deptService.updateDeptById(entity);
        return R.ok().put("result", "success");
    }
}
