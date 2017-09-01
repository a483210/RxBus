package com.xiuyukeji.rxbus;

/**
 * 订阅信息序列接口
 *
 * @author Created by jz on 2016/12/7 13:36
 */
interface SubscriberInfoIndex {
    SubscriberInfo getIndex(Class<?> subscriberType);
}
