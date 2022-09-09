package com.sw961174314.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "message")
public class MessageEntity implements Serializable {
    @Id
    private String _id;

    @Indexed(unique = true)
    private String uuid;

    @Indexed
    private Integer senderId;

    private String senderPhoto="https://sw961174314-1305711112.cos.ap-guangzhou.myqcloud.com/img/banner/sw961174314.jpg";

    private String senderName;

    @Indexed
    private Date sendTime;

    private String msg;
}

