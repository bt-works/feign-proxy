package org.devil.feign.sample.controller;

import org.devil.feign.sample.service.TestClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author yaojun
 * 2020/5/28 11:37
 */
@RestController
public class IndexController {

    @Resource
    private TestClientService testClientService;

    @GetMapping("/index")
    public String index(){
        return testClientService.index();
    }
}
