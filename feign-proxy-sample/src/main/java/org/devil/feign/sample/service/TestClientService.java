package org.devil.feign.sample.service;

import org.devil.feign.sample.client.TestClient;
import org.springframework.stereotype.Service;

/**
 * @author yaojun
 * 2020/5/28 11:14
 */
@Service
public class TestClientService implements TestClient {

    @Override
    public String index() {
        return "123122";
    }


}
