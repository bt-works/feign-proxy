package org.devil.feign.sample.service;

import io.swagger.annotations.ApiOperation;
import org.devil.feign.sample.client.TestClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * @author yaojun
 * 2020/5/28 11:14
 */
@Service
public class TestClientService implements TestClient, InitializingBean {

    @Resource
    private RestTemplate restTemplate;

    @Override
    public String index() {
       return restTemplate.postForEntity("http://127.0.0.1:8999/test/index/post",Collections.singletonMap("111","222"),String.class).getBody();
    }

    @Override
    public String testPost(Map<String, String> params) {
        return new Random().nextInt(2000)+"";
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("111111nn11111111111111"+restTemplate);

    }
}
