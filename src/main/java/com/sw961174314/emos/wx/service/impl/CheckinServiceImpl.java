package com.sw961174314.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.sw961174314.emos.wx.config.SystemConstants;
import com.sw961174314.emos.wx.db.dao.*;
import com.sw961174314.emos.wx.db.pojo.TbCheckin;
import com.sw961174314.emos.wx.db.pojo.TbFaceModel;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.CheckinService;
import com.sw961174314.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Scope("prototype")
@Slf4j
public class CheckinServiceImpl implements CheckinService {

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;

    @Value("${emos.code}")
    private String code;

    @Autowired
    private SystemConstants constants;

    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private EmailTask emailTask;

    /**
     * 检测当天是否可以签到
     * @param userId
     * @param date
     * @return
     */
    @Override
    public String validCanCheckIn(int userId, String date) {
        // 当前这天是不是特殊的节假日
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        // 当前这天是不是特殊的工作日
        boolean bool_2 = workdayDao.searchTodayIsWorkday() != null ? true : false;
        String type = "工作日";
        // 判断当前日期是不是周末
        if (DateUtil.date().isWeekend()) {
            type = "节假日";
        }
        if (bool_1) {
            type = "节假日";
        } else if (bool_2) {
            type = "工作日";
        }
        if (type.equals("节假日")) {
            return "节假日不需要考勤";
        }else {
            DateTime now = DateUtil.date();
            String start = DateUtil.today() + " " + constants.attendanceStartTime;
            String end = DateUtil.today() + " " + constants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "没有到上班考勤开始时间";
            } else if (now.isAfter(attendanceEnd)) {
                return "超过了上班考勤结束时间";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    /**
     * 执行签到功能
     * @param param
     */
    @Override
    public void checkin(HashMap param) {
        // 当前时间
        Date d1 = DateUtil.date();
        // 上班时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceTime);
        // 上班考勤结束时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
        // 正常考勤 status为1
        int status = 1;
        if (d1.compareTo(d2) <= 0) {
            // 当前时间小于上班时间 正常考勤
            status = 1;
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            // 当前时间大于上班时间&&当前时间小于上班考勤结束时间 迟到
            status = 2;
        } else {
            throw new EmosException("超出考勤时间段，无法考勤");
        }
        int userId = (int) param.get("userId");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel == null) {
            throw new EmosException("不存在人脸模型");
        } else {
            // 人脸识别图片地址
            String path = (String) param.get("path");
            // 发起Http请求 检测该人脸模型与系统内保存的人脸模型是否一致
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            // 上传图片和人脸模型数据
            request.form("photo", FileUtil.file(path), "targetModel", faceModel);
            // 绑定密钥
            request.form("code", code);
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            String body = response.body();
            System.err.println("body" + body);
            // 识别人脸
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("签到无效，非本人签到");
            } else {
                // risk:0->常态化防控 1->低风险 2->中风险 3->高风险
                int risk = 0;
                // 查询城市 区县
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                // 获取地址
                String address = (String) param.get("address");
                String country = (String) param.get("country");
                String province = (String) param.get("province");
                System.err.println(param);
                if (!StrUtil.isBlank(city) && !StrUtil.isBlank(district)) {
                    String code = cityDao.searchCode(city);
                    try {
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        // 得到Http请求
                        Document document = Jsoup.connect(url).get();
                        // 查询div标签
                        Elements elements = document.getElementsByClass("list-content");
                        if (elements.size() > 0) {
                            Element element = elements.get(0);
                            // 找到div标签内最后一个p标签
                            String result = element.select("p:last-child").text();
                            if ("高风险".equals(result)) {
                                risk = 3;
                                // 发送警告邮件
                                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("dept_name");
                                // 如果当前员工没有部门则置空
                                deptName = deptName != null ? deptName : "";
                                SimpleMailMessage message = new SimpleMailMessage();
                                // 发送警告邮件给hr
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "曾去过高风险疫情地区");
                                message.setText(deptName + "员工" + name + ',' + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + ",该地区属于新冠疫情高风险地区,请及时与该员工练习,核实情况!");
                                emailTask.sendAsync(message);
                            } else if ("中风险".equals(result)) {
                                risk = 2;
                            } else if ("低风险".equals(result)) {
                                risk = 1;
                            }
                        }
                    } catch (Exception e) {
//                        log.error("执行异常", e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                // 保存签到记录
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }
    }

    /**
     * 创建人脸模型
     * @param userId
     * @param path
     */
    @Override
    public void createFaceModel(int userId, String path) {
        // 发出Http请求
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        // 上传图片
        request.form("photo", FileUtil.file(path));
        // 绑定密钥
        request.form("code", code);
        HttpResponse response = request.execute();
        // 保存响应体
        String body = response.body();
        if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
            throw new EmosException(body);
        } else {
            TbFaceModel entity = new TbFaceModel();
            entity.setUserId(userId);
            entity.setFaceModel(body);
            faceModelDao.insert(entity);
        }
    }

    /**
     * 查询员工的基本信息与签到情况
     * @param userId
     * @return
     */
    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap map = checkinDao.searchTodayCheckin(userId);
        return map;
    }

    /**
     * 统计员工签到次数
     * @param userId
     * @return
     */
    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }

    /**
     * 查询员工本周的签到情况
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param);
        // 查询本周特殊的节假日
        ArrayList holidaysList = holidaysDao.searchHolidayInRange(param);
        // 查询本周特殊的工作日
        ArrayList workdyaList = workdayDao.searchWorkdayInRange(param);
        // 生成本周的七天对象
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        ArrayList<HashMap> list = new ArrayList<>();
        range.forEach(one->{
            String date = one.toString("yyyy-MM-dd");
            // 判断是否是周末（初步判断是工作日还是节假日）
            String type = "工作日";
            if (one.isWeekend()) {
                type = "节假日";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "节假日";
            } else if (workdyaList != null && workdyaList.contains(date)) {
                type = "工作日";
            }
            String status = "";
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) {
                // 判断当天是工作日并且在当天的时间以内（DateUtil.compare得到的结果小于或等于0 意味着这一天已经发生过）
                status = "缺勤";
                // 考勤标记 如果已考勤 则flag的值为true
                boolean flag = false;
                for (HashMap<String, String> map : checkinList) {
                    // 获取当天的考勤
                    if (map.containsValue(date)) {
                        status = map.get("status");
                        flag = true;
                        break;
                    }
                }
                DateTime endTime = DateUtil.parse(DateUtil.today() + " " + constants.attendanceEndTime);
                String today = DateUtil.today();
                // 如果没有考勤 && 签到的时间为今天 && 当前时间在结束时间之前
                if (flag == false && date.equals(today) && DateUtil.date().isBefore(endTime)) {
                    status = "";
                }
            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    /**
     * 查询员工月考勤记录
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        return this.searchWeekCheckin(param);
    }
}
