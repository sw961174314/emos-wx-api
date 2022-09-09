package com.sw961174314.emos.wx.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.sw961174314.emos.wx.common.util.R;
import com.sw961174314.emos.wx.config.shiro.JwtUtil;
import com.sw961174314.emos.wx.controller.form.*;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Api("用户模块Web接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    /**
     * 保存token令牌
     *
     * @param token
     * @param userId
     */
    private void saveCacheToken(String token, int userId) {
        redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
    }

    /**
     * 用户注册
     *
     * @param form
     * @return
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public R register(@Valid @RequestBody RegisterForm form) {
        int userId = userService.registerUser(form.getRegisterCode(), form.getCode(), form.getNickname(), form.getPhoto());
        String token = jwtUtil.createToken(userId);
        Set<String> permsSet = userService.searchUserPermissions(userId);
        saveCacheToken(token, userId);
        return R.ok("用户注册成功").put("token", token).put("permission", permsSet);
    }

    /**
     * 用户登录
     *
     * @param form
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("登录系统")
    public R login(@Valid @RequestBody LoginForm form, @RequestHeader("token") String token) {
        Integer userId;
        // 判断token是否为非空
        if (StrUtil.isNotEmpty(token)) {
            try {
                // 验证令牌的有效性
                jwtUtil.verifierToken(token);
            } catch (TokenExpiredException e) {
                // 如果令牌过期就生成新的令牌
                userId = userService.login(form.getCode());
                token = jwtUtil.createToken(userId);
                saveCacheToken(token, userId);
            }
            userId = jwtUtil.getUserId(token);
        } else {
            // 更新令牌
            userId = userService.login(form.getCode());
            token = jwtUtil.createToken(userId);
            saveCacheToken(token, userId);
        }
        Set<String> permsSet = userService.searchUserPermissions(userId);
        return R.ok("登录成功").put("token", token).put("permission", permsSet);
    }

    /**
     * 查询用户摘要信息
     *
     * @param token
     * @return
     */
    @GetMapping("/searchUserSummary")
    @ApiOperation("查询用户摘要信息")
    public R searchUserSummary(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        HashMap map = userService.searchUserSummary(userId);
        return R.ok().put("result", map);
    }

    /**
     * 查询员工列表(按照部门分组排列)
     *
     * @param form
     * @return
     */
    @PostMapping("/searchUserGroupByDept")
    @ApiOperation("查询员工列表，按照部门分组排列")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:SELECT"}, logical = Logical.OR)
    public R searchUserGroupByDept(@Valid @RequestBody SearchUserGroupByDeptForm form) {
        ArrayList<HashMap> list = userService.searchUserGroupByDept(form.getKeyword());
        return R.ok().put("result", list);
    }

    /**
     * 查询会议成员信息
     *
     * @param form
     * @return
     */
    @PostMapping("/searchMembers")
    @ApiOperation("查询成员")
    @RequiresPermissions(value = {"ROOT", "MEETING:INSERT", "MEETING:UPDATE"}, logical = Logical.OR)
    public R searchMembers(@Valid @RequestBody SearchMembersForm form) {
        if (!JSONUtil.isJsonArray(form.getMembers())) {
            throw new EmosException("members不是JSON数组");
        }
        List param = JSONUtil.parseArray(form.getMembers()).toList(Integer.class);
        ArrayList list = userService.searchMembers(param);
        return R.ok().put("result", list);
    }

    /**
     * 查询用户姓名和头像
     *
     * @param form
     * @return
     */
    @PostMapping("/selectUserPhotoAndName")
    @ApiOperation("查询用户姓名和头像")
    @RequiresPermissions(value = {"WORKFLOW:APPROVAL"})
    public R selectUserPhotoAndName(@Valid @RequestBody SelectUserPhotoAndNameForm form) {
        if (!JSONUtil.isJsonArray(form.getIds())) {
            throw new EmosException("参数不是JSON数组");
        }
        List<Integer> param = JSONUtil.parseArray(form.getIds()).toList(Integer.class);
        List<HashMap> list = userService.selectUserPhotoAndName(param);
        return R.ok().put("result", list);
    }

    /**
     * 添加员工数据
     * @param form
     * @return
     */
    @PostMapping("/insertUser")
    @ApiOperation("添加员工数据")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:INSERT"}, logical = Logical.OR)
    public R inserUser(@RequestBody InsertUserForm form) {
        if (!JSONUtil.isJsonArray(form.getRole())) {
            throw new EmosException("角色不是数组形式");
        }
        JSONArray array = JSONUtil.parseArray(form.getRole());
        HashMap param = new HashMap();
        param.put("name",form.getName());
        param.put("sex",form.getSex());
        param.put("tel",form.getTel());
        param.put("email",form.getEmail());
        param.put("hiredate",form.getHiredate());
        param.put("role",form.getRole());
        param.put("deptName",form.getDeptName());
        param.put("status",form.getStatus());
        param.put("createTime",new Date());
        if (array.contains(0)) {
            param.put("root", true);
        } else {
            param.put("root", false);
        }
        userService.insertUser(param);
        return R.ok().put("result", "success");
    }

    /**
     * 查询员工数据
     * @param form
     * @return
     */
    @PostMapping("/searchUserInfo")
    @ApiOperation("查询员工数据")
    @RequiresPermissions(value = {"ROOT","EMPLOYEE:SELECT"},logical = Logical.OR)
    public R searchUserInfo(@Valid @RequestBody SearchUserInfoForm form) {
        HashMap map = userService.searchUserInfo(form.getUserId());
        return R.ok().put("result", map);
    }

    /**
     * 查询员工数据
     * @param token
     * @return
     */
    @GetMapping("/searchUserSelfInfo")
    @ApiOperation("查询员工数据")
    public R searchUserSelfInfo(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        HashMap map = userService.searchUserInfo(userId);
        return R.ok().put("result", map);
    }

    /**
     * 更新员工数据
     * @param form
     * @return
     */
    @PostMapping("/updateUserInfo")
    @ApiOperation("更新用户数据")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:UPDATE"}, logical = Logical.OR)
    public R updateUserInfo(@Valid @RequestBody UpdateUserInfoForm form) {
        boolean root = false;
        if (!JSONUtil.isJsonArray(form.getRole())) {
            throw new EmosException("role不是有效的JSON数组");
        } else {
            JSONArray role = JSONUtil.parseArray(form.getRole());
            root = role.contains(0) ? true : false;
        }
        HashMap param = new HashMap();
        param.put("name", form.getName());
        param.put("sex", form.getSex());
        param.put("deptName", form.getDeptName());
        param.put("tel", form.getTel());
        param.put("email", form.getEmail());
        param.put("hiredate", form.getHiredate());
        param.put("role", form.getRole());
        param.put("status", form.getStatus());
        param.put("userId", form.getUserId());
        param.put("root", root);
        int rows = userService.updateUserInfo(param);
        return R.ok().put("result", rows);
    }

    /**
     * 删除员工记录
     * @param form
     * @return
     */
    @PostMapping("/deleteUserById")
    @ApiOperation("删除员工记录")
    @RequiresPermissions(value = {"ROOT","EMPLOYEE:DELETE"},logical = Logical.OR)
    public R deleteUserById(@Valid @RequestBody DeleteUserByIdForm form) {
        userService.deleteUserById(form.getId());
        return R.ok().put("result", "success");
    }

    /**
     * 查询员工列表(通讯录)
     * @return
     */
    @GetMapping("/searchUserContactList")
    @ApiOperation("查询员工列表(通讯录)")
    public R searchUserContactList() {
        JSONObject json = userService.searchUserContactList();
        return R.ok().put("result", json);
    }
}
