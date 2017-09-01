package com.xiuyukeji.rxbus;

/**
 * 保存订阅信息
 *
 * @author Created by jz on 2016/12/7 13:35
 */

class SubscriberInfo {
    final Class<?> subscriberType;
    final SubscriberMethodInfo[] subscriberMethodInfos;

    SubscriberInfo(Class<?> subscriberType, SubscriberMethodInfo[] subscriberMethodInfo) {
        this.subscriberType = subscriberType;
        this.subscriberMethodInfos = subscriberMethodInfo;
    }
}
