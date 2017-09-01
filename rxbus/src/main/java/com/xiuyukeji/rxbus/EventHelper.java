package com.xiuyukeji.rxbus;

import com.xiuyukeji.rxbus.lifecycle.EventRxLifecycleHelper;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.xiuyukeji.rxbus.utils.SubscriberUtils.applySchedulers;

/**
 * 普通事件帮助类
 *
 * @author Created by jz on 2016/12/27 14:24
 */
class EventHelper {
    //根据Tag存储Subject，在post时会查找符合的Subject发布事件
    //从这个角度上来讲，同一个subject订阅越多则发布事件越慢，反之如果tag越多则订阅事件速度越慢
    private final ConcurrentHashMap<Integer, Subject<Object>> bus;
    //管理subject取消订阅
    private final EventRxLifecycleHelper lifecycleHelper;
    private final RecycleHelper recycleHelper;

    EventHelper() {
        this.bus = new ConcurrentHashMap<>();
        this.lifecycleHelper = new EventRxLifecycleHelper();
        this.recycleHelper = new RecycleHelper(bus);
    }

    //发布普通事件
    void post(Object event, int tag) {
        Subject<Object> subject = bus.get(tag);
        if (subject != null) {
            subject.onNext(event);
        }
    }

    //是否已经注册
    boolean isRegistered(Object subscriber) {
        return lifecycleHelper.contains(subscriber);
    }

    //是否有订阅该标识的事件
    boolean hasSubscriberForTag(int tag) {
        Subject<Object> subject = bus.get(tag);
        return subject != null && subject.hasObservers();
    }

    //订阅事件
    void subscribe(final Object subscriber, final SubscriberMethodInfo info) {
        obtainSubject(info.tag)
                .compose(applySchedulers(info.mode))
                .filter(Functions.isInstanceOf(info.eventType))
                .compose(lifecycleHelper.bindUntilEvent(subscriber))
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

    //注销普通事件
    void unregister(Object subscriber) {
        lifecycleHelper.unbindEvent(subscriber);
    }

    //通过tag创建不同的subject，以提高post时的速度，subject被订阅的事件越多速度越慢
    Observable<Object> obtainSubject(final int tag) {
        Subject<Object> subject = bus.get(tag);
        if (subject == null) {
            subject = PublishSubject.create().toSerialized();
            bus.put(tag, subject);
        }
        return subject.doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                recycleHelper.recycle(tag);
            }
        });
    }

    //清除所有事件，测试专用
    void clearCaches() {
        bus.clear();
        lifecycleHelper.clear();
    }
}
