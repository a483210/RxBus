package com.xiuyukeji.rxbus.utils;

import com.xiuyukeji.rxbus.ThreadMode;

import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * 订阅工具类
 *
 * @author Created by jz on 2016/12/27 13:30
 */
public class SubscriberUtils {
    /**
     * 判断是否是system类
     *
     * @param name 名称
     */
    public static boolean isSystemClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.");
    }

    /**
     * 根据数据类型name和tag生成key
     *
     * @param eventType 数据类型
     * @param tag       标识
     * @return key
     */
    public static String obtainEventKey(Class<?> eventType, int tag) {
        return String.format(Locale.getDefault(), "%s_%d", eventType.getName(), tag);
    }

    /**
     * 指定线程，默认为发送线程
     *
     * @param mode 线程模式
     */
    public static <T> ObservableTransformer<T, T> applySchedulers(final ThreadMode mode) {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> upstream) {
                if (mode == ThreadMode.MAIN) {
                    return upstream.observeOn(AndroidSchedulers.mainThread());
                } else if (mode == ThreadMode.ASYNC) {
                    return upstream.observeOn(Schedulers.newThread());
                } else if (mode == ThreadMode.IO) {
                    return upstream.observeOn(Schedulers.io());
                } else if (mode == ThreadMode.SINGLE) {
                    return upstream.observeOn(Schedulers.single());
                } else {
                    return upstream;
                }
            }
        };
    }

    /**
     * 反射创建对象
     *
     * @param name 类名
     */
    public static Object newInstance(String name) {
        try {
            return Class.forName(name).newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
