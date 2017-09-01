package com.xiuyukeji.rxbus;

import android.support.annotation.NonNull;

import com.xiuyukeji.rxbus.utils.CacheList;
import com.xiuyukeji.rxbus.utils.SubscriberUtils;

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
    private final SubscriberInfoIndex subscriberInfoIndex;
    //搜索SubscriberInfo时的缓存
    private final CacheList<SubscriberInfo> cacheList;

    final EventHelper eventHelper;
    final EventStickyHelper eventStickyHelper;

    private RxBus() {
        //SubscriberInfoIndexImpl名字固定，请不要修改
        this.subscriberInfoIndex = (SubscriberInfoIndex) SubscriberUtils.newInstance("com.xiuyukeji.rxbus.SubscriberInfoIndexImpl");

        this.eventHelper = new EventHelper();
        this.eventStickyHelper = new EventStickyHelper();

        this.cacheList = new CacheList<>(new SubscriberInfo[30]);
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
    public void post(@NonNull Object event, int tag) {
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
    public void postSticky(@NonNull Object event, int tag) {
        eventStickyHelper.postSticky(event, tag);
        post(event, tag);
    }

    public void postStickySingle(@NonNull Object event) {
        postStickySingle(event, EventType.DEFAULT_TAG);
    }

    /**
     * 发布只会被消费一次的sticky事件，数据类型和标志需要对应
     *
     * @param event 数据
     * @param tag   标识
     */
    public void postStickySingle(@NonNull Object event, int tag) {
        eventStickyHelper.postStickySingle(event, tag);
        post(event, tag);
    }

    public <T> T getStickyEvent(@NonNull Class<T> eventType) {
        return getStickyEvent(eventType, EventType.DEFAULT_TAG);
    }

    /**
     * 根据类型和标识查找发布的sticky事件
     *
     * @param eventType 数据类型
     * @param tag       标识
     */
    public <T> T getStickyEvent(@NonNull Class<T> eventType, int tag) {
        return eventStickyHelper.getStickyEvent(eventType, tag);
    }

    public <T> T removeStickyEvent(@NonNull T event) {
        return removeStickyEvent(event, EventType.DEFAULT_TAG);
    }

    public <T> T removeStickyEvent(@NonNull T event, int tag) {
        return (T) eventStickyHelper.removeStickyEvent(event.getClass(), tag);
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
    public <T> T removeStickyEvent(@NonNull Class<T> eventType, int tag) {
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
        if (subscriberInfoIndex == null) {
            return;
        }
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
            SubscriberInfo info = subscriberInfoIndex.getIndex(clazz);
            if (info != null) {
                cacheList.add(subscriberInfoIndex.getIndex(clazz));
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
        if (subscriberInfoIndex == null) {
            return;
        }
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
    public boolean hasSubscriberForTag(int tag) {
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