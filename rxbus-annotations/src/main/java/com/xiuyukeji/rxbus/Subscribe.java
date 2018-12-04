package com.xiuyukeji.rxbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RxBus注解
 *
 * @author Created by jz on 2016/12/7 10:15
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Subscribe {
    String tag() default EventType.DEFAULT_TAG;

    ThreadMode mode() default ThreadMode.POST;

    boolean sticky() default false;
}