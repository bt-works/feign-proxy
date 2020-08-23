package org.devil.feign.sample.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author yaojun
 * 2020/5/28 11:14
 */
@Aspect
@Component
public class SampleAop {

    private final static Logger logger = LoggerFactory.getLogger(SampleAop.class);

    @Pointcut("execution(public * org.devil.feign.sample.service.TestClientService.index())")
    public void point(){

    }

    @Before(value = "point()")
    public void before(JoinPoint joinPoint){
        logger.info("{},{},{}",joinPoint.getKind(),joinPoint.getSignature().getDeclaringTypeName(),joinPoint.getSourceLocation());
    }
}
