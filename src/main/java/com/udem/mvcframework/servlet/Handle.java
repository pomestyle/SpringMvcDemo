package com.udem.mvcframework.servlet;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 封装handler方法相关的信息
 * <p>
 * handler参数相关信息
 */
public class Handle {


    /**
     * method.invoke(obj 需要执行得对象
     */
    private Object controller;

    /**
     * url正则匹配对象
     */
    private Pattern pattern;

    /**
     * 需要调用得url对应得方法
     */
    private Method method;

    /**
     * 存储参数顺序，为了进行参数绑定  Integer第几个参数
     */
    private Map<String, Integer> map;


    public Handle(Object controller, Pattern pattern, Method method) {
        this.controller = controller;
        this.pattern = pattern;
        this.method = method;
        map = new HashMap<>();
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }
}
