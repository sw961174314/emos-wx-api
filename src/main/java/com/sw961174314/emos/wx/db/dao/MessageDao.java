package com.sw961174314.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.mongodb.client.result.DeleteResult;
import com.sw961174314.emos.wx.db.pojo.MessageEntity;
import com.sw961174314.emos.wx.db.pojo.MessageRefEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

import java.util.Date;

@Repository
public class MessageDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 新增消息（需要转换时间）
     * @param entity
     * @return
     */
    public String insert(MessageEntity entity) {
        // 将北京时间转换为格林尼治时间
        Date sendTime = entity.getSendTime();
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, 8);
        entity.setSendTime(sendTime);
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

    /**
     * 按照分页查询消息
     * @param userId
     * @param start
     * @param length
     * @return
     */
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {
        JSONObject json = new JSONObject();
        json.set("$toString", "$_id");
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields().addField("id").withValue(json).build(),
                Aggregation.lookup("message_ref","id","messageId","ref"),
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC,"sendTime")),
                Aggregation.skip(start),
                Aggregation.limit(length)
        );
        // 联合查询
        AggregationResults<HashMap> results = mongoTemplate.aggregate(aggregation, "message", HashMap.class);
        List<HashMap> list = results.getMappedResults();
        list.forEach(one->{
            List<MessageRefEntity> refList = (List<MessageRefEntity>) one.get("ref");
            MessageRefEntity entity = refList.get(0);
            Boolean readFlag = entity.getReadFlag();
            String refId = entity.get_id();
            one.put("readFlag", readFlag);
            one.put("refId", refId);
            one.remove("ref");
            one.remove("_id");
            Date sendTime = (Date) one.get("sendTime");
            sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);
            String today = DateUtil.today();
            if (today.equals(DateUtil.date(sendTime).toDateStr())) {
                one.put("sendTime", DateUtil.format(sendTime, "HH:mm"));
            } else {
                one.put("sendTime", DateUtil.format(sendTime, "yyyy/MM/dd"));
            }
        });
        return list;
    }

    /**
     * 根据id查找数据
     * @param id
     * @return
     */
    public HashMap searchMessageById(String id) {
        HashMap map = mongoTemplate.findById(id, HashMap.class, "message");
        Date sendTime = (Date) map.get("sendTime");
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);
        map.replace("sendTime", DateUtil.format(sendTime, "yyyy-MM-dd HH:mm"));
        return map;
    }

    /**
     * 删除消息主题
     * @param receiverId
     * @return
     */
    public long deleteUserMessage(int receiverId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(receiverId));
        DeleteResult result = mongoTemplate.remove(query, "message");
        long rows = result.getDeletedCount();
        return rows;
    }
}
