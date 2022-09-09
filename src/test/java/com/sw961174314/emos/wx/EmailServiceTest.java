package com.sw961174314.emos.wx;

import com.sw961174314.emos.wx.task.ActiveCodeTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailServiceTest{

    @Autowired
    private ActiveCodeTask activeCodeTask;

    @Test
    public void sendSimpleEmail(){
        activeCodeTask.sendActiveCodeAsync(49,"961174314@qq.com");
    }
}

