package org.devil.feign.sample;

import org.devil.feign.sample.client.TestClient;
import org.devil.proxy.annotation.EnableAutoProxyFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yaojun
 * 2020/5/28 11:11
 */
@EnableAutoProxyFeign(basePackageClasses = TestClient.class,enable = false)
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        new SpringApplication(SampleApplication.class).run(args);
    }
}
