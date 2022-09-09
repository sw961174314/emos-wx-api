package com.sw961174314.emos.wx.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sw961174314.emos.wx.db.dao.*;
import com.sw961174314.emos.wx.db.pojo.MessageEntity;
import com.sw961174314.emos.wx.db.pojo.TbUser;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.UserService;
import com.sw961174314.emos.wx.task.ActiveCodeTask;
import com.sw961174314.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

@Service
@Slf4j
@Scope("prototype")
public class UserServiceImpl implements UserService {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    @Autowired
    private MessageTask messageTask;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbDeptDao deptDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private MessageRefDao messageRefDao;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private ActiveCodeTask activeCodeTask;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取OpenId
     * @param code
     * @return
     */
    private String getOpenId(String code){
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid",appId);
        map.put("secret",appSecret);
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String response = HttpUtil.post(url, map);
        // 将响应字符串转化为JSON对象
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登录凭证错误");
        }
        return openId;
    }

    /**
     * 注册新用户
     * @param registerCode
     * @param code
     * @param nickname
     * @return
     */
    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
        // 如果邀请码是000000 代表是超级管理员
        if (registerCode.equals("000000")) {
            // 查询超级管理员账户是否已经绑定
            boolean bool = userDao.haveRootUser();
            if (!bool) {
                // 把当前用户绑定到Root账户
                String openId = getOpenId(code);
                HashMap param = new HashMap();
                param.put("openId",openId);
                param.put("nickname",nickname);
                param.put("photo",photo);
                param.put("role","[0]");
                param.put("status",1);
                param.put("createTime",new Date());
                param.put("root",true);
                userDao.insert(param);
                int id = userDao.searchIdByOpenId(openId);
                // 注册成功后发送提示消息
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
                entity.setSenderName("系统消息");
                entity.setUuid(IdUtil.simpleUUID());
                entity.setMsg("欢迎您注册成为超级管理员，请及时更新你的员工个人信息");
                entity.setSendTime(new Date());
                messageTask.sendAsync(id + "",entity);
                return id;
            }else {
                // 如果Root已经绑定 就抛出异常
                throw new EmosException("无法绑定超级管理员账号");
            }
        } else if (!redisTemplate.hasKey(registerCode)) {
            // 判断邀请码是否有效
            throw new EmosException("不存在这个激活码");
        } else {
            int userId = Integer.parseInt(String.valueOf(redisTemplate.opsForValue().get(registerCode)));
            // 把当前用户绑定到Root账户
            TbUser entity = new TbUser();
            String openId = getOpenId(code);
            HashMap param = new HashMap();
            param.put("openId", openId);
            param.put("nickname", nickname);
            param.put("photo", photo);
            param.put("userId", userId);
            int row = userDao.activeUserAccount(param);
            if (row != 1) {
                throw new EmosException("账号激活失败");
            }
            redisTemplate.delete(registerCode);
            return userId;
        }
    }

    /**
     * 查询用户的权限列表
     * @param userId
     * @return
     */
    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions=userDao.searchUserPermissions(userId);
        return permissions;
    }

    /**
     * 用户登录
     * @param code
     * @return
     */
    @Override
    public Integer login(String code) {
        String openId = getOpenId(code);
        Integer id = userDao.searchIdByOpenId(openId);
        if (id == null) {
            throw new EmosException("帐户不存在");
        }
        // 登录成功后接收提示消息
        messageTask.receiveAsync(id + "");
        return id;
    }

    /**
     * Shiro认证功能
     * @param userId
     * @return
     */
    @Override
    public TbUser searchById(int userId) {
        TbUser user = userDao.searchById(userId);
        return user;
    }

    /**
     * 查询员工的入职日期
     * @param userId
     * @return
     */
    @Override
    public String searchUserHiredate(int userId) {
        String hiredate = userDao.searchUserHiredate(userId);
        return hiredate;
    }

    /**
     * 查询用户的概要信息
     * @param userId
     * @return
     */
    @Override
    public HashMap searchUserSummary(int userId) {
        HashMap map = userDao.searchUserSummary(userId);
        return map;
    }

    /**
     * 查询部门成员
     * @param keyword
     * @return
     */
    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {
        // 部门数据
        ArrayList<HashMap> list_1 = deptDao.searchDeptMembers(keyword);
        // 员工数据
        ArrayList<HashMap> list_2 = userDao.searchUserGroupByDept(keyword);
        for (HashMap map_1 : list_1) {
            long deptId = (long) map_1.get("id");
            ArrayList members = new ArrayList();
            for (HashMap map_2 : list_2) {
                long id = (long) map_2.get("deptId");
                if (deptId == id) {
                    members.add(map_2);
                }
            }
            map_1.put("members", members);
        }
        return list_1;
    }

    /**
     * 查询会议成员信息
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        ArrayList<HashMap> list = userDao.searchMembers(param);
        return list;
    }

    /**
     * 查询审批人的头像和姓名
     * @param param
     * @return
     */
    @Override
    public List<HashMap> selectUserPhotoAndName(List param) {
        List<HashMap> list = userDao.selectUserPhotoAndName(param);
        return list;
    }

    /**
     * 添加普通员工
     * @param param
     */
    @Override
    public void insertUser(HashMap param) {
        // 保存记录
        int row = userDao.insert(param);
        if (row == 1) {
            String email = (String) param.get("email");
            // 根据Email查找新添加用户的主键值
            int userId = userDao.searchUserIdByEmail(email);
            // 生成激活码 并且用邮箱发送
            activeCodeTask.sendActiveCodeAsync(userId, email);
        } else {
            throw new EmosException("员工数据添加失败");
        }
    }

    /**
     * 查询员工基本信息
     * @param userId
     * @return
     */
    @Override
    public HashMap searchUserInfo(int userId) {
        HashMap map = userDao.searchUserInfo(userId);
        return map;
    }

    /**
     * 更新员工数据
     * @param param
     * @return
     */
    @Override
    public int updateUserInfo(HashMap param) {
        // 更新员工数据
        int rows = userDao.updateUserInfo(param);
        // 更新成功就发送消息通知
        if (rows == 1) {
            Integer userId = (Integer) param.get("userId");
            String msg = "你的个人资料已经被成功修改";
            MessageEntity entity = new MessageEntity();
            // 系统自动发出
            entity.setSenderId(0);
            entity.setSenderPhoto("../../static/system.jpg");
            entity.setSenderName("系统消息");
            entity.setMsg(msg);
            entity.setSendTime(new Date());
            messageTask.sendAsync(userId.toString(),entity);
        }
        return 0;
    }

    /**
     * 删除员工数据
     * @param id
     */
    @Override
    public void deleteUserById(int id) {
        // 删除员工数据
        int row = userDao.deleteUserById(id);
        if (row != 1) {
            throw new EmosException("删除员工失败");
        }
        checkinDao.deleteUserCheckin(id);
        messageDao.deleteUserMessage(id);
        messageRefDao.deleteUserMessageRef(id);
        faceModelDao.deleteFaceModel(id);
        messageTask.deleteQueue(id + "");
    }

    /**
     * 查询员工列表(通讯录)
     * @return
     */
    @Override
    public JSONObject searchUserContactList() {
        ArrayList<HashMap> list = userDao.searchUserContactList();
        String letter = null;
        JSONObject json = new JSONObject(true);
        JSONArray array = null;
        for (HashMap<String, String> map : list) {
            String name = map.get("name");
            String firstLetter = PinyinUtil.getPinyin(name).charAt(0) + "";
            firstLetter = firstLetter.toUpperCase();
            if (letter == null || !letter.equals(firstLetter)) {
                letter = firstLetter;
                array = new JSONArray();
                json.set(letter, array);
            }
            array.put(map);
        }
        return json;
    }
}
