package com.xiuyukeji.rxbus;

import android.content.Context;

import com.xiuyukeji.rxbus.utils.CacheList;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import static com.xiuyukeji.rxbus.utils.SubscriberUtils.isSystemClass;


/**
 * 事件总线，代替EventBus使用
 * 采用Apt生成序列信息，提高注册效率
 *
 * @author Created by jz on 2016/12/6 17:05
 */
public class RxBus {

    private static volatile RxBus instance;

    public static RxBus get() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) {
                    instance = new RxBus();
                }
            }
        }
        return instance;
    }

    public static final String TAG = "rxBus";

    //存储订阅信息，通过APT自动生成，提高注册速度
    private final SubscriberLoader subscriberLoader;
    //搜索SubscriberInfo时的缓存
    private final CacheList<SubscriberInfo> cacheList;

    final EventHelper eventHelper;
    final EventStickyHelper eventStickyHelper;

    private RxBus() {
        this.eventHelper = new EventHelper();
        this.eventStickyHelper = new EventStickyHelper();

        this.cacheList = new CacheList<>(new SubscriberInfo[30]);

        this.subscriberLoader = new SubscriberLoader();
    }

    /**
     * 默认通过反射扫描工程然后添加订阅序列，反射在加固后将无效
     * 或者使用rxbus-register自动注册，可以提高注册速度
     *
     * @param context 上下文
     */
    public void registerIndex(Context context) {
        synchronized (this) {
            subscriberLoader.autoRegisterIndex();//如果启用rxbus-register
            subscriberLoader.registerIndex(context);
        }
    }

    /**
     * 添加订阅序列
     *
     * @param index 序列
     */
    public void addIndex(SubscriberIndex index) {
        synchronized (this) {
            subscriberLoader.addIndex(index);
        }
    }

    public void post(@NonNull Object event) {
        post(event, EventType.DEFAULT_TAG);
    }

    /**
     * 发布事件，数据类型和标志需要对应
     *
     * @param event 数据
     * @param tag   标识
     */
    public void post(@NonNull Object event, String tag) {
        eventHelper.post(event, tag);
    }

    public void postSticky(@NonNull Object event) {
        postSticky(event, EventType.DEFAULT_TAG);
    }

    /**
     * 发布sticky事件，数据类型和标志需要对应
     * 将数据保存至stickyEvents，在register时调用
     *
     * @param event 数据
     * @param tag   标识
     */
    public void postSticky(@NonNull Object event, String tag) {
        eventStickyHelper.postSticky(event, tag);
        post(event, tag);
    }

    @CheckResult
    public <T> T getStickyEvent(@NonNull Class<T> eventType) {
        return getStickyEvent(eventType, EventType.DEFAULT_TAG);
    }

    /**
     * 根据类型和标识查找发布的sticky事件的值
     *
     * @param eventType 数据类型
     * @param tag       标识
     */
    @CheckResult
    public <T> T getStickyEvent(@NonNull Class<T> eventType, String tag) {
        return eventStickyHelper.getStickyEvent(eventType, tag);
    }

    public <T> T removeStickyEvent(@NonNull T event) {
        return removeStickyEvent(event, EventType.DEFAULT_TAG);
    }

    /**
     * 删除sticky事件，数据和标志需要对应
     *
     * @param event 数据
     * @param tag   标识
     */
    public <T> T removeStickyEvent(@NonNull T event, String tag) {
        return (T) removeStickyEvent(event.getClass(), tag);
    }

    public <T> T removeStickyEvent(@NonNull Class<T> eventType) {
        return removeStickyEvent(eventType, EventType.DEFAULT_TAG);
    }

    /**
     * 删除sticky事件，数据类型和标志需要对应
     *
     * @param eventType 数据类型
     * @param tag       标识
     */
    public <T> T removeStickyEvent(@NonNull Class<T> eventType, String tag) {
        return eventStickyHelper.removeStickyEvent(eventType, tag);
    }

    /**
     * 删除全部sticky事件
     */
    public void removeStickyEventAll() {
        eventStickyHelper.removeStickyEventAll();
    }

    /**
     * 注册
     *
     * @param subscriber 订阅者
     */
    public void register(@NonNull final Object subscriber) {
        if (isRegistered(subscriber)) {
            return;
        }

        synchronized (this) {
            CacheList<SubscriberInfo> cache = searchInfo(subscriber);
            if (cache.isEmpty()) {
                return;
            }

            int count = cache.size();
            for (int i = 0; i < count; i++) {
                SubscriberMethodInfo[] methodMethods = cache.get(i).subscriberMethodInfos;
                if (methodMethods == null || methodMethods.length == 0) {
                    continue;
                }

                for (SubscriberMethodInfo info : methodMethods) {
                    eventHelper.subscribe(subscriber, info);
                    if (info.sticky) {
                        eventStickyHelper.runStickyEvent(subscriber, info);
                    }
                }
            }
        }
    }

    private CacheList<SubscriberInfo> searchInfo(Object subscriber) {
        cacheList.clear();
        Class<?> clazz = subscriber.getClass();
        while (clazz != null && !isSystemClass(clazz.getName())) {
            SubscriberInfo info = subscriberLoader.getInfo(clazz);
            if (info != null) {
                cacheList.add(info);
            }
            clazz = clazz.getSuperclass();
        }
        return cacheList;
    }

    /**
     * 解除所有绑定在subscriber上的订阅事件
     *
     * @param subscriber 订阅者
     */
    public void unregister(@NonNull Object subscriber) {
        if (!isRegistered(subscriber)) {
            return;
        }

        synchronized (this) {
            eventHelper.unregister(subscriber);
        }
    }

    /**
     * 判断是否注册过
     *
     * @param subscriber 订阅者
     */
    @CheckResult
    public boolean isRegistered(@NonNull Object subscriber) {
        synchronized (this) {
            return eventHelper.isRegistered(subscriber);
        }
    }

    /**
     * 判断是否有事件被订阅
     *
     * @param tag 标识
     */
    @CheckResult
    public boolean hasSubscriberForTag(String tag) {
        return eventHelper.hasSubscriberForTag(tag);
    }

    /**
     * 清除所有数据，测试专用
     */
    public void clearCaches() {
        eventHelper.clearCaches();
        removeStickyEventAll();
    }
}