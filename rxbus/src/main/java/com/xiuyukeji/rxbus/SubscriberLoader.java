package com.xiuyukeji.rxbus;

import android.content.Context;

import com.xiuyukeji.rxbus.utils.SubscriberUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 加载订阅信息数据
 *
 * @author Created by jz on 2018/3/23 13:30
 */
class SubscriberLoader {
    private final HashMap<Class<?>, SubscriberInfo> subscriberInfo;
    private final HashMap<Class<?>, SubscriberIndex> subscriberIndex;

    private boolean isRegisteredIndex;//不允许重复注册

    SubscriberLoader() {
        this.subscriberInfo = new HashMap<>();
        this.subscriberIndex = new HashMap<>();
    }

    //rxbus-register
    void autoRegisterIndex() {
        if (isRegisteredIndex) {
            return;
        }
    }

    //反射注册订阅信息
    void registerIndex(Context context) {
        if (isRegisteredIndex) {
            return;
        }

        isRegisteredIndex = true;

        Class<?> clazz = SubscriberIndex.class;
        List<Class<?>> list = SubscriberUtils.getClassName(context, String.format("%sImpl", clazz.getName()), clazz);
        if (list == null) {
            return;
        }
        for (Class<?> cls : list) {
            SubscriberIndex index = SubscriberUtils.newInstance(cls);
            addIndex(index);
        }
    }

    void addIndex(SubscriberIndex index) {
        if (index != null) {
            subscriberIndex.put(index.getClass(), index);
        }
    }

    SubscriberInfo getInfo(Class<?> subscriberClass) {
        SubscriberInfo info = subscriberInfo.get(subscriberClass);
        if (info == null) {
            for (SubscriberIndex index : subscriberIndex.values()) {
                info = index.readInfo(subscriberClass);
                if (info != null) {
                    putIndex(info);
                    break;
                }
            }
        }
        return info;
    }

    private void putIndex(SubscriberInfo info) {
        subscriberInfo.put(info.subscriberType, info);
    }
}
