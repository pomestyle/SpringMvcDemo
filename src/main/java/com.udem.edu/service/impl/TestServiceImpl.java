package com.udem.edu.service.impl;

import com.udem.edu.service.TestService;
import com.udem.mvcframework.annotation.Service;

/**
 * @author Pilgrim
 */
@Service(value = "testServiceImpl")
public class TestServiceImpl implements TestService {

    @Override
    public String test(String name) {
        System.out.println("-------------------- name = " + name);
        return "--------- " + name;
    }

}
