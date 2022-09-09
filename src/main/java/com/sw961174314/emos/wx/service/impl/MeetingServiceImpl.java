package com.sw961174314.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sw961174314.emos.wx.db.dao.TbMeetingDao;
import com.sw961174314.emos.wx.db.dao.TbUserDao;
import com.sw961174314.emos.wx.db.pojo.TbMeeting;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {

    @Value("${emos.code}")
    private String code;

    @Value("${emos.recieveNotify}")
    private String recieveNotify;

    @Value("${workflow.url}")
    private String workflow;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbMeetingDao meetingDao;

    /**
     * 创建会议
     * @param entity
     */
    @Override
    public void insertMeeting(TbMeeting entity) {
        // 保存数据
        int row = meetingDao.insertMeeting(entity);
        if (row != 1) {
            throw new EmosException("会议添加失败");
        }
        // 开启审批工作流
        startMeetingWorkflow(entity.getUuid(),entity.getCreatorId().intValue(),entity.getDate(),entity.getStart());
    }

    /**
     * 分页查询
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchMyMeetingListByPage(param);
        String date = null;
        // 会议小列表
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;
        JSONArray array = null;
        // 分组
        for (HashMap map : list) {
            // 获取会议日期
            String temp = map.get("date").toString();
            // 与前一个会议的时间进行比较 如果不相同则开辟新空间
            if (!temp.equals(date)) {
                date = temp;
                // 开启新的小列表
                resultMap = new HashMap();
                resultMap.put("date", date);
                array = new JSONArray();
                resultMap.put("list",array);
                resultList.add(resultMap);
            }
            // 往array里放入当前的会议记录
            array.put(map);
        }
        return resultList;
    }

    private void startMeetingWorkflow(String uuid, int creatorId, String date, String start) {
        HashMap info = userDao.searchUserInfo(creatorId);
        // 给工作流发送数据
        JSONObject json = new JSONObject();
        json.set("url", recieveNotify);
        json.set("uuid", uuid);
        json.set("openId", info.get("openId"));
        json.set("code", code);
        json.set("date", date);
        json.set("start", start);
        String[] roles = info.get("roles").toString().split(",");
        if (!ArrayUtil.contains(roles, "总经理")) {
            // 查询部门经理的id
            Integer managerId = userDao.searchDeptManagerId(creatorId);
            json.set("managerId", managerId);
            // 查询总经理的id
            Integer gmId = userDao.searchGmId();
            json.set("gmId", gmId);
            // 查询参会人员是否隶属同一部门
            boolean bool = meetingDao.searchMeetingMembersInSameDept(uuid);
            json.set("sameDept", bool);
        }
        String url = workflow + "/workflow/startMeetingProcess";
        HttpResponse resp = HttpRequest.post(url).header("Content-Type", "application/json").body(json.toString()).execute();
        if (resp.getStatus() == 200) {
            json = JSONUtil.parseObj(resp.body());
            String instanceId = json.getStr("instanceId");
            HashMap param = new HashMap();
            param.put("uuid", uuid);
            param.put("instanceId", instanceId);
            int row = meetingDao.updateMeetingInstanceId(param);
            if (row != 1) {
                throw new EmosException("保存会议工作流实例ID失败");
            }
        }
    }

    /**
     * 保存会议的基本信息与参会人员信息
     * @param id
     * @return
     */
    @Override
    public HashMap searchMeetingById(int id) {
        HashMap map = meetingDao.searchMeetingById(id);
        ArrayList<HashMap> list = meetingDao.searchMeetingMembers(id);
        map.put("members", list);
        return map;
    }

    /**
     * 更新会议信息
     * @param param
     */
    @Override
    public void updateMeetingInfo(HashMap param) {
        int id = (int) param.get("id");
        String date = param.get("date").toString();
        String start = param.get("start").toString();
        String instanceId = param.get("instanceId").toString();
        // 查询会议记录
        HashMap oldMeeting = meetingDao.searchMeetingById(id);
        String uuid = (String) oldMeeting.get("uuid");
        // 获取创建人id
        Integer createorId = Integer.parseInt(oldMeeting.get("creatorId").toString());
        int row = meetingDao.updateMeetingInfo(param);
        if (row != 1) {
            throw new EmosException("会议更新失败");
        }
        // 删除已有的工作流
        JSONObject json = new JSONObject();
        json.set("instanceId", instanceId);
        json.set("reason", "会议被修改");
        json.set("uuid", uuid);
        json.set("code", code);
        String url = workflow + "/workflow/deleteProcessById";
        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json").body(json.toString()).execute();
        if (resp.getStatus() != 200) {
            log.error("删除工作流失败");
            throw new EmosException("删除工作流失败");
        }
        startMeetingWorkflow(uuid,createorId,date,start);
    }

    /**
     * 删除会议记录
     * @param id
     */
    @Override
    public void deleteMeetingById(int id) {
        HashMap meeting = meetingDao.searchMeetingById(id); //查询会议信息
        String uuid = meeting.get("uuid").toString();
        String instanceId = meeting.get("instanceId").toString();
        DateTime date = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
        DateTime now = DateUtil.date();
        //会议开始前20分钟，不能删除会议
        if(now.isAfterOrEquals(date.offset(DateField.MINUTE,-20))){
            throw new EmosException("距离会议开始不足20分钟，不能删除会议");
        };
        int row = meetingDao.deleteMeetingById(id);
        if (row != 1) {
            throw new EmosException("会议删除失败");
        }
        //删除会议工作流
        JSONObject json = new JSONObject();
        json.set("instanceId", instanceId);
        json.set("reason", "会议被取消");
        json.set("code",code);
        json.set("uuid",uuid);
        String url = workflow+"/workflow/deleteProcessById";
        HttpResponse resp = HttpRequest.post(url).header("content-type", "application/json").body(json.toString()).execute();
        if (resp.getStatus() != 200) {
            log.error("删除工作流失败");
            throw new EmosException("删除工作流失败");
        }
    }

    /**
     * 查询开会的日期
     * @param param
     * @return
     */
    @Override
    public List<String> searchUserMeetingInMonth(HashMap param) {
        List list=meetingDao.searchUserMeetingInMonth(param);
        return list;
    }
}
