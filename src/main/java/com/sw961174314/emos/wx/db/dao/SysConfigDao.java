package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import java.util.*;

@Mapper
public interface SysConfigDao {
    public List<SysConfig> selectAllParam();
}
