package com.xiuyukeji.rxbus;

/**
 * 保存订阅方法信息
 *
 * @author Created by jz on 2016/12/7 14:19
 */
class SubscriberMethodInfo {
    final String tag;
    final boolean sticky;
    final ThreadMode mode;
    final Class<?> eventType;
    final OnCallListener listener;

    SubscriberMethodInfo(String tag, boolean sticky, ThreadMode mode, Class<?> eventType, OnCallListener listener) {
        this.tag = tag;
        this.sticky = sticky;
        this.mode = mode;
        this.eventType = eventType;
        this.listener = listener;
    }
}
