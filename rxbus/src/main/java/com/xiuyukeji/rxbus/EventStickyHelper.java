package com.xiuyukeji.rxbus;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

import static com.xiuyukeji.rxbus.utils.SubscriberUtils.applySchedulers;
import static com.xiuyukeji.rxbus.utils.SubscriberUtils.getEventKey;

/**
 * sticky事件帮助类
 *
 * @author Created by jz on 2016/12/27 14:24
 */
class EventStickyHelper {
    private final ConcurrentHashMap<String, StickyEventInfo> stickyEvents;

    EventStickyHelper() {
        this.stickyEvents = new ConcurrentHashMap<>();
    }

    //发布sticky事件
    void postSticky(Object event, int tag) {
        String key = getEventKey(event.getClass(), tag);
        stickyEvents.put(key, new StickyEventInfo(event));
    }

    //发布只会被消费一次的sticky事件
    void postStickySingle(Object event, int tag) {
        String key = getEventKey(event.getClass(), tag);
        stickyEvents.put(key, new StickyEventInfo(true, event));
    }

    <T> T getStickyEvent(Class<T> eventType, int tag) {
        String key = getEventKey(eventType, tag);
        StickyEventInfo info = stickyEvents.get(key);
        if (info != null) {
            return eventType.cast(info.event);
        }
        return null;
    }

    //删除sticky事件
    <T> T removeStickyEvent(Class<T> eventType, int tag) {
        String key = getEventKey(eventType, tag);
        StickyEventInfo info = stickyEvents.remove(key);
        if (info != null) {
            return eventType.cast(info.event);
        }
        return null;
    }

    //删除所有sticky事件
    void removeStickyEventAll() {
        stickyEvents.clear();
    }

    //运行sticky事件
    void runStickyEvent(final Object subscriber, final SubscriberMethodInfo info) {
        String key = getEventKey(info.eventType, info.tag);
        final StickyEventInfo stickyInfo = stickyEvents.get(key);
        if (stickyInfo == null) {
            return;
        }

        Observable.just(stickyInfo.event)
                .compose(applySchedulers(info.mode))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object value) throws Exception {
                        info.listener.onCall(subscriber, value);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (stickyInfo.isSingle) {
                            removeStickyEvent(info.eventType, info.tag);
                        }
                        throwable.printStackTrace();
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        if (stickyInfo.isSingle) {
                            removeStickyEvent(info.eventType, info.tag);
                        }
                    }
                });
    }
}
