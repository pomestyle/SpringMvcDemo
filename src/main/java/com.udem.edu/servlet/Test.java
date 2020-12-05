package com.udem.edu.servlet;

import com.udem.edu.service.TestService;
import com.udem.mvcframework.annotation.Autowired;
import com.udem.mvcframework.annotation.Controller;
import com.udem.mvcframework.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequestMapping("/api")
@Controller
public class Test {

    @Autowired
    private TestService testService;

    @RequestMapping("/test")
    public String test(HttpServletRequest request, HttpServletResponse response, String name) {

        return testService.test(name);
    }
}
