package com.sw961174314.emos.wx.service;

import java.util.ArrayList;
import java.util.HashMap;

public interface CheckinService {

    // 检测当天是否可以签到
    String validCanCheckIn(int userId, String date);

    // 执行签到功能
    void checkin(HashMap param);

    // 创建人脸模型
    void createFaceModel(int userId, String path);

    // 查询员工的基本信息与签到情况
    public HashMap searchTodayCheckin(int userId);

    // 统计员工签到次数
    public long searchCheckinDays(int userId);

    // 查询员工本周的签到情况
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);

    // 查询员工月考勤记录
    ArrayList<HashMap> searchMonthCheckin(HashMap param);
}
