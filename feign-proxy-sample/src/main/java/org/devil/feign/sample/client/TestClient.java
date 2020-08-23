package org.devil.feign.sample.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author yaojun
 * 2020/5/28 11:12
 */
@FeignClient(name = "testClient",path = "/test")
public interface TestClient {

    @GetMapping("/index/225")
    public abstract String index();
}
