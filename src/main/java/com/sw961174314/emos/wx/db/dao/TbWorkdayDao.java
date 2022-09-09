package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbWorkday;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbWorkdayDao {

    // 查询当天是否是工作日
    public Integer searchTodayIsWorkday();

    // 查询特殊工作日
    public ArrayList<String> searchWorkdayInRange(HashMap param);
}

