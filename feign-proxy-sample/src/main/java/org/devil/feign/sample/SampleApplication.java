package org.devil.feign.sample;

import org.devil.feign.sample.client.TestClient;
import org.devil.proxy.annotation.EnableAutoProxyFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * @author yaojun
 * 2020/5/28 11:11
 */
//@EnableFeignClients
@EnableAutoProxyFeign(basePackageClasses = TestClient.class,enable = true)
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        new SpringApplication(SampleApplication.class).run(args);
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
