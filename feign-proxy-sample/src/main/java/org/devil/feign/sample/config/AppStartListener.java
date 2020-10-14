package org.devil.feign.sample.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppStartListener implements ApplicationListener<ApplicationStartedEvent> {

    private Logger logger = LoggerFactory.getLogger(AppStartListener.class);

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationEvent) {
        Environment environment = applicationEvent.getApplicationContext().getEnvironment();
        String appName = environment.getProperty("spring.application.name");
        String port = environment.getProperty("server.port");
        String contextPath = environment.getProperty("server.servlet.context-path");
        logger.info(appName + " App start,host:http://127.0.0.1:"+port+"/"+(contextPath == null?"":contextPath));
    }
}
