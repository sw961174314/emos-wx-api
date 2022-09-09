package com.sw961174314.emos.wx.service;

import com.sw961174314.emos.wx.db.pojo.TbMeeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MeetingService {

    // 保存数据
    void insertMeeting(TbMeeting entity);

    // 分页查询
    ArrayList<HashMap> searchMyMeetingListByPage(HashMap param);

    // 保存会议的基本信息与参会人员信息
    HashMap searchMeetingById(int id);

    // 更新会议信息
    void updateMeetingInfo(HashMap param);

    // 删除会议记录
    void deleteMeetingById(int id);

    // 查询开会的日期
    List<String> searchUserMeetingInMonth(HashMap param);
}
