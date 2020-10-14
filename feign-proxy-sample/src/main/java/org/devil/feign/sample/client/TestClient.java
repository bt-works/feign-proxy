package org.devil.feign.sample.client;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * @author yaojun
 * 2020/5/28 11:12
 */
@Api(tags = "test 测试 feign-auto-proxy")
@FeignClient(name = "testClient",path = "/test",qualifier = "testClient")
public interface TestClient {

    @ApiOperation(value = "测试",notes = "feign-auto-proxy test")
    @GetMapping("/index/225")
    public abstract String index();

    @PostMapping("/index/post")
    public String testPost(@RequestBody Map<String,String> params);
}
