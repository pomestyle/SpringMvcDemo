package com.udem.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * 类或者方法上
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
