package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbHolidays;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbHolidaysDao {

    // 查询当天是否是节假日
    public Integer searchTodayIsHolidays();

    // 查询特殊节假日
    public ArrayList<String> searchHolidayInRange(HashMap param);
}

