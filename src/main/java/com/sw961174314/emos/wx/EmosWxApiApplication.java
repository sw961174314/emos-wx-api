package com.sw961174314.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.sw961174314.emos.wx.config.SystemConstants;
import com.sw961174314.emos.wx.db.dao.SysConfigDao;
import com.sw961174314.emos.wx.db.pojo.SysConfig;
import com.sw961174314.emos.wx.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan
@Slf4j
@EnableAsync
public class EmosWxApiApplication {

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private SystemConstants constants;

    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }

    @PostConstruct
    public void init() {
        List<SysConfig> list = sysConfigDao.selectAllParam();
        list.forEach(one->{
            String key = one.getParamKey();
            key = StrUtil.toCamelCase(key);
            String value = one.getParamValue();
            // 通过反射的方式给变量进行赋值
            try {
                Field field = constants.getClass().getDeclaredField(key);
                field.set(constants,value);
            } catch (Exception e) {
                log.error("执行异常",e);
            }
        });
        new File(imageFolder).mkdirs();
    }
}
