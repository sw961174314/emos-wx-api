package com.sw961174314.emos.wx.task;

import com.rabbitmq.client.*;
import com.sw961174314.emos.wx.db.pojo.MessageEntity;
import com.sw961174314.emos.wx.db.pojo.MessageRefEntity;
import com.sw961174314.emos.wx.exception.EmosException;
import com.sw961174314.emos.wx.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MessageTask {

    @Autowired
    private ConnectionFactory factory;

    @Autowired
    private MessageService messageService;

    /**
     * 发送消息(同步)
     * @param topic
     * @param entity
     */
    public void send(String topic, MessageEntity entity) {
        String id = messageService.insert(entity);
        try(
            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
        ){
            // 连接topic队列 channel.queueDeclare(队列名称,是否持久化,是否排外,是否自动删除,其他参数(没有则设置为null))
            channel.queueDeclare(topic, true, false, false, null);
            HashMap map = new HashMap();
            map.put("messageId", id);
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(map).build();
            // 发送信息
            channel.basicPublish("", topic, properties, entity.getMsg().getBytes());
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("向MQ发送消息失败");
        }
    }

    /**
     * 发送消息(异步)
     * @param topic
     * @param entity
     */
    @Async
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

    /**
     * 接收消息(同步)
     * @param topic
     * @return
     */
    public int receive(String topic) {
        int i = 0;
        try(
            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
        ){
            // 连接topic队列 channel.queueDeclare(队列名称,是否持久化,是否排外,是否自动删除,其他参数(没有则设置为null))
            channel.queueDeclare(topic, true, false, false, null);
            while (true) {
                // 接收topic里的数据
                GetResponse response = channel.basicGet(topic, false);
                if (response != null) {
                    // 从响应中获取绑定的数据
                    AMQP.BasicProperties properties = response.getProps();
                    // 获取请求头的数据
                    Map<String, Object> map = properties.getHeaders();
                    // 从map中获取绑定的messageId
                    String messageId = map.get("messageId").toString();
                    // 获取消息的正文
                    byte[] body = response.getBody();
                    String message = new String(body);
                    log.debug("从RabbitMQ接收的消息：" + message);
                    // 往Ref集合中保存数据
                    MessageRefEntity entity = new MessageRefEntity();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
                    messageService.insertRef(entity);
                    // 返回ACK应答
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                    i++;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("接收消息失败");
        }
        return i;
    }

    /**
     * 接收消息(异步)
     * @param topic
     * @return
     */
    @Async
    public int receiveAsync(String topic) {
        return receive(topic);
    }

    /**
     * 删除消息(同步)
     * @param topic
     */
    public void deleteQueue(String topic) {
        try(
            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
        ){
            channel.queueDelete(topic);
            log.error("消息队列成功删除");
        }catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("删除消息失败");
        }
    }

    /**
     * 删除消息(异步)
     * @param topic
     */
    @Async
    public void deleteQueueAsync(String topic) {
        deleteQueue(topic);
    }
}
