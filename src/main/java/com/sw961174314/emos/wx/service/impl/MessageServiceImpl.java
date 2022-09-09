package com.sw961174314.emos.wx.service.impl;

import com.sw961174314.emos.wx.db.dao.MessageDao;
import com.sw961174314.emos.wx.db.dao.MessageRefDao;
import com.sw961174314.emos.wx.db.pojo.MessageEntity;
import com.sw961174314.emos.wx.db.pojo.MessageRefEntity;
import com.sw961174314.emos.wx.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private MessageRefDao messageRefDao;

    /**
     * 向Message集合中插入数据
     * @param entity
     * @return
     */
    @Override
    public String insert(MessageEntity entity) {
        String id = messageDao.insert(entity);
        return id;
    }

    /**
     * 向Ref集合中插入数据
     * @param entity
     * @return
     */
    @Override
    public String insertRef(MessageRefEntity entity) {
        String id = messageRefDao.insert(entity);
        return id;
    }

    /**
     * 查询未读消息的数量
     * @param userId
     * @return
     */
    @Override
    public long searchUnreadCount(int userId) {
        long count = messageRefDao.searchUnreadCount(userId);
        return count;
    }

    /**
     * 查询接收到最新消息的数量
     * @param userId
     * @return
     */
    @Override
    public long searchLastCount(int userId) {
        long count = messageRefDao.searchLastCount(userId);
        return count;
    }

    /**
     * 查询分页数据
     * @param userId
     * @param start
     * @param length
     * @return
     */
    @Override
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {
        List<HashMap> list = messageDao.searchMessageByPage(userId, start, length);
        return list;
    }

    /**
     * 根据id查询消息
     * @param id
     * @return
     */
    @Override
    public HashMap searchMessageById(String id) {
        HashMap map = messageDao.searchMessageById(id);
        return map;
    }

    /**
     * 修改消息状态
     * @param id
     * @return
     */
    @Override
    public long updateUnreadMessage(String id) {
        long rows = messageRefDao.updateUnreadMessage(id);
        return rows;
    }

    /**
     * 根据id删除消息
     * @param id
     * @return
     */
    @Override
    public long deleteMessageRefById(String id) {
        long rows = messageRefDao.deleteMessageRefById(id);
        return rows;
    }

    /**
     * 根据userId删除消息
     * @param userId
     * @return
     */
    @Override
    public long deleteUserMessageRef(int userId) {
        long rows = messageRefDao.deleteUserMessageRef(userId);
        return rows;
    }
}
