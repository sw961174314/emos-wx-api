package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbCheckinDao {

    // 查询签到记录
    public Integer haveCheckin(HashMap param);

    // 保存签到记录
    public void insert(TbCheckin entity);

    // 查询员工的基本信息和当天的签到结果
    public HashMap searchTodayCheckin(int userId);

    // 统计员工的签到天数
    public long searchCheckinDays(int userId);

    // 统计本周的考勤情况
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);

    // 删除签到数据
    public int deleteUserCheckin(int userId);
}

