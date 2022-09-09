package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbMeeting;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface TbMeetingDao {
    // 新增会议
    public int insertMeeting(TbMeeting entity);

    // 分页查询
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);

    // 查询参会人员是不是同一个部门
    public boolean searchMeetingMembersInSameDept(String uuid);

    // 更新会议记录
    public int updateMeetingInstanceId(HashMap map);

    // 查询会议基本信息
    public HashMap searchMeetingById(int id);

    // 查询会议的参会人
    public ArrayList<HashMap> searchMeetingMembers(int id);

    // 更新会议信息
    public int updateMeetingInfo(HashMap param);

    // 删除会议
    public int deleteMeetingById(int id);

    // 查询开会的日期
    public List<String> searchUserMeetingInMonth(HashMap param);
}
