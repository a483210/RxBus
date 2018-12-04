package com.xiuyukeji.rxbus.utils;

import android.content.Context;

import com.xiuyukeji.rxbus.ThreadMode;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import dalvik.system.DexFile;
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
     * 查找以clazz命名的并且继承clazz的类
     *
     * @param context 上下文
     * @param clazz   类
     */
    public static List<Class<?>> getClassName(Context context, String pathName, Class<?> clazz) {
        try {
            List<Class<?>> classNameList = new ArrayList<>();

            DexFile dexfile = new DexFile(context.getApplicationContext().getPackageCodePath());
            Enumeration<String> enumeration = dexfile.entries();
            while (enumeration.hasMoreElements()) {
                String className = enumeration.nextElement();

                if (className.contains(pathName)) {
                    Class<?> cls = Class.forName(className);
                    if (clazz.isAssignableFrom(cls)) {
                        classNameList.add(cls);
                    }
                }
            }

            return classNameList;
        } catch (Throwable e) {
            return null;
        }
    }

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
    public static String obtainEventKey(Class<?> eventType, String tag) {
        return String.format(Locale.getDefault(), "%s_%s", eventType.getName(), tag);
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
    public static <T> T newInstance(String name) {
        try {
            return newInstance(Class.forName(name));
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 反射创建对象
     *
     * @param clazz 类
     */
    public static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Throwable e) {
            return null;
        }
    }
}
