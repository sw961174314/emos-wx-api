package com.sw961174314.emos.wx.task;

import cn.hutool.core.util.RandomUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 生成激活码并发送邮件
 */
@Component
@Scope("prototype")
public class ActiveCodeTask {

    @Autowired
    private EmailTask emailTask;

    @Autowired
    private RedisTemplate redisTemplate;

    @Async("AsyncTaskExecutor")
    public void sendActiveCodeAsync(int userId, String email) {
        String activeCode = null;
        while (true) {
            // 生成激活码
            activeCode = RandomUtil.randomInt(100000, 999999) + "";
            // 判断是否有key所对应的值 有则返回true 没有则返回false(找到没有生成过的激活码)
            if (redisTemplate.hasKey(activeCode)) {
                continue;
            } else {
                break;
            }
        }
        // 激活码有效时间为1天
        redisTemplate.opsForValue().set(activeCode, userId + "", 1, TimeUnit.DAYS);
        // 发送邮件
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("EMOS系统新员工激活码");
        msg.setText("你的新员工激活码是:" + activeCode);
        emailTask.sendAsync(msg);
    }
}
