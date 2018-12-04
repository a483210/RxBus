package com.xiuyukeji.rxbus;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

import static com.xiuyukeji.rxbus.utils.SubscriberUtils.applySchedulers;
import static com.xiuyukeji.rxbus.utils.SubscriberUtils.obtainEventKey;

/**
 * sticky事件帮助类
 *
 * @author Created by jz on 2016/12/27 14:24
 */
class EventStickyHelper {
    private final ConcurrentHashMap<String, Object> stickyEvents;

    EventStickyHelper() {
        this.stickyEvents = new ConcurrentHashMap<>();
    }

    //发布sticky事件
    void postSticky(Object event, String tag) {
        String key = obtainEventKey(event.getClass(), tag);
        stickyEvents.put(key, event);
    }

    <T> T getStickyEvent(Class<T> eventType, String tag) {
        String key = obtainEventKey(eventType, tag);
        Object event = stickyEvents.get(key);
        if (event != null) {
            return eventType.cast(event);
        }
        return null;
    }

    //删除sticky事件
    <T> T removeStickyEvent(Class<T> eventType, String tag) {
        String key = obtainEventKey(eventType, tag);
        Object event = stickyEvents.remove(key);
        if (event != null) {
            return eventType.cast(event);
        }
        return null;
    }

    //删除所有sticky事件
    void removeStickyEventAll() {
        stickyEvents.clear();
    }

    //运行sticky事件
    void runStickyEvent(final Object subscriber, final SubscriberMethodInfo info) {
        String key = obtainEventKey(info.eventType, info.tag);
        Object event = stickyEvents.get(key);
        if (event == null) {
            return;
        }

        Observable.just(event)
                .compose(applySchedulers(info.mode))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object value) throws Exception {
                        info.listener.onCall(subscriber, value);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }
}
