package com.xiuyukeji.rxbus.lifecycle;

import android.support.annotation.NonNull;

import com.trello.rxlifecycle2.LifecycleTransformer;

import java.util.HashSet;

/**
 * RxLifecycle帮助类，存储object
 *
 * @author Created by jz on 2016/12/30 15:23
 */
public class EventRxLifecycleHelper extends BaseRxLifecycleHelper<Object> {
    private final HashSet<Object> subscribers;

    public EventRxLifecycleHelper() {
        this.subscribers = new HashSet<>();
    }

    @NonNull
    @Override
    public <T> LifecycleTransformer<T> bindUntilEvent(@NonNull Object event) {
        subscribers.add(event);
        return super.bindUntilEvent(event);
    }

    @Override
    public void unbindEvent(@NonNull Object event) {
        subscribers.remove(event);
        super.unbindEvent(event);
    }

    public boolean contains(@NonNull Object event) {
        return subscribers.contains(event);
    }

    public HashSet<Object> asSet() {
        return new HashSet<>(subscribers);
    }

    public void clear() {
        subscribers.clear();
    }
}
