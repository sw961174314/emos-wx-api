package com.sw961174314.emos.wx.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory getFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        // Linux主机的IP地址
        factory.setHost("175.178.131.4");
        // RabbitMQ端口号
        factory.setPort(5672);
        return factory;
    }
}
