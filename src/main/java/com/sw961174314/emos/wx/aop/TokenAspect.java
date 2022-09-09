package com.sw961174314.emos.wx.aop;

import com.sw961174314.emos.wx.common.util.R;
import com.sw961174314.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TokenAspect {

    @Autowired
    private ThreadLocalToken threadLocalToken;

    /**
     * 拦截哪些方法的调用
     */
    @Pointcut("execution(public * com.sw961174314.emos.wx.controller.*.*(..)))")
    public void aspect() {

    }

    @Around("aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 方法执行结果
        R r = (R) point.proceed();
        String token = threadLocalToken.getToken();
        // 如果ThredLocal中存在Token 说明是更新的Token
        if (token != null) {
            // 往响应中放置Token
            r.put("token", token);
            threadLocalToken.clear();
        }
        return r;
    }
}
