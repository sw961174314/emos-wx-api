package com.sw961174314.emos.wx.service;

import com.sw961174314.emos.wx.db.pojo.MessageEntity;
import com.sw961174314.emos.wx.db.pojo.MessageRefEntity;

import java.util.*;

public interface MessageService {

    // 向Message集合中插入数据
    String insert(MessageEntity entity);

    // 向Ref集合中插入数据
    String insertRef(MessageRefEntity entity);

    // 查询未读消息的数量
    long searchUnreadCount(int userId);

    // 查询接收到最新消息的数量
    long searchLastCount(int userId);

    // 查询分页数据
    List<HashMap> searchMessageByPage(int userId, long start, int length);

    // 根据id查询消息
    HashMap searchMessageById(String id);

    // 修改消息状态
    long updateUnreadMessage(String id);

    // 根据id删除消息
    long deleteMessageRefById(String id);

    // 根据userId删除消息
    long deleteUserMessageRef(int userId);
}
