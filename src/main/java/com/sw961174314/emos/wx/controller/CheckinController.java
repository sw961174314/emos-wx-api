package com.sw961174314.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.sw961174314.emos.wx.common.util.R;
import com.sw961174314.emos.wx.config.SystemConstants;
import com.sw961174314.emos.wx.config.shiro.JwtUtil;
import com.sw961174314.emos.wx.controller.form.CheckinForm;
import com.sw961174314.emos.wx.controller.form.SearchMonthCheckinForm;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.CheckinService;
import com.sw961174314.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@RequestMapping("/checkin")
@RestController
@Api("签到模块Web接口")
@Slf4j
public class CheckinController {

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private SystemConstants constants;

    /**
     * 检测用户是否可以签到
     *
     * @param token
     * @return
     */
    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用户今天是否可以签到")
    public R validCanCheckIn(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        String result = checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }

    @PostMapping("/checkin")
    @ApiOperation("签到")
    public R checkin(@Valid CheckinForm form, @RequestParam("photo") MultipartFile file, @RequestHeader("token") String token) {
        if (file == null) {
            return R.error("没有上传文件");
        }
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase();
        if (!fileName.endsWith(".jpg")) {
            return R.error("必须提交JPG格式图片");
        } else {
            // 保存图片
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path));
                HashMap param = new HashMap();
                param.put("userId",userId);
                param.put("path",path);
                param.put("city",form.getCity());
                param.put("district",form.getDistrict());
                param.put("address",form.getAddress());
                param.put("country",form.getCountry());
                param.put("province",form.getProvince());
                checkinService.checkin(param);
                return R.ok("签到成功");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("图片保存错误");
            }
            // 签到成功 删除文件
            finally {
                FileUtil.del(path);
            }
        }
    }

    /**
     * 创建人脸模型
     * @param file
     * @param token
     * @return
     */
    @PostMapping("/createFaceModel")
    @ApiOperation("创建人脸模型")
    public R createFaceModel(@RequestParam("photo") MultipartFile file, @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        if (file == null) {
            return R.error("没有上传文件");
        }
        // 获取图片的后缀
        String fileName = file.getOriginalFilename().toLowerCase();
        // 设置存放地址的路径
        String path = imageFolder + "/" + fileName;
        if (!fileName.endsWith(".jpg")) {
            return R.error("必须提交JPG格式图片");
        } else {
            try{
                file.transferTo(Paths.get(path));
                // 创建人脸模型
                checkinService.createFaceModel(userId, path);
                return R.ok("人脸建模成功");
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new EmosException("保存图片错误");
            }finally {
                // 删除签到图片
                FileUtil.del(path);
            }
        }
    }

    /**
     * 查询用户当日签到数据
     * @param token
     * @return
     */
    @GetMapping("/searchTodayCheckin")
    @ApiOperation("查询用户当日签到数据")
    public HashMap searchTodayCheckin(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        HashMap map = checkinService.searchTodayCheckin(userId);
        // 当天考勤的开始时间
        map.put("attendanceTime", constants.attendanceTime);
        // 当天考勤的结束时间
        map.put("closingTime", constants.closingTime);
        // 统计员工的签到天数
        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays", days);
        // 判断日期是否在用户入职之前
        // 员工的入职日期
        DateTime hiredate = DateUtil.parse(userService.searchUserHiredate(userId));
        // 本周的开始日期
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        // 本周的结束日期
        DateTime endDate = DateUtil.endOfWeek(DateUtil.date());
        if (startDate.isBefore(hiredate)) {
            startDate = hiredate;
        }
        HashMap param = new HashMap();
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        param.put("userId", userId);
        ArrayList<HashMap> list = checkinService.searchWeekCheckin(param);
        map.put("weekCheckin", list);
        return R.ok().put("result", map);
    }

    @PostMapping("/searchMonthCheckin")
    @ApiOperation("查询员工某月的签到数据")
    public R searchMonthCheckin(@Valid @RequestBody SearchMonthCheckinForm form, @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        DateTime hiredate = DateUtil.parse(userService.searchUserHiredate(userId));
        String month = form.getMonth() < 10 ? "0" + form.getMonth() : String.valueOf(form.getMonth());
        DateTime startDate = DateUtil.parse(form.getYear() + "-" + month + "-01");
        // 判断某年某月的第一天是否在员工入职的范围之前
        if (startDate.isBefore(DateUtil.beginOfMonth(hiredate))) {
            throw new EmosException("只能查询考勤之后日期的数据");
        }
        if (startDate.isBefore(hiredate)) {
            startDate = hiredate;
        }
        // 获取当月最后一天
        DateTime endDate = DateUtil.endOfMonth(startDate);
        HashMap param = new HashMap();
        param.put("userId", userId);
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        ArrayList<HashMap> list = checkinService.searchMonthCheckin(param);
        int sum_1 = 0,sum_2 = 0,sum_3 = 0;
        // 统计考勤情况
        for (HashMap<String, String> one : list) {
            // type 节假日/工作日
            String type = one.get("type");
            // status 考勤状态
            String status = one.get("status");
            if ("工作日".equals(type)) {
                if ("正常".equals(status)) {
                    sum_1 = sum_1 + 1;
                } else if ("迟到".equals(status)) {
                    sum_2 = sum_2 + 1;
                } else if ("缺勤".equals(status)) {
                    sum_3 = sum_3 + 1;
                }
            }
        }
        return R.ok().put("list",list).put("sum_1",sum_1).put("sum_2",sum_2).put("sum_3",sum_3);
    }
}
