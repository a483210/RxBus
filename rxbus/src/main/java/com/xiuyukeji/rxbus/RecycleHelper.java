package com.xiuyukeji.rxbus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.Subject;

/**
 * subject回收帮助类
 *
 * @author Created by jz on 2017/2/9 13:09
 */
class RecycleHelper {
    private static final int RECYCLE_SPACE_TIME = 5 * 1000;

    private final ConcurrentSkipListSet<String> cache;

    private final ConcurrentHashMap<String, Subject<Object>> bus;

    private volatile boolean isStartRecycle;

    RecycleHelper(ConcurrentHashMap<String, Subject<Object>> bus) {
        this.cache = new ConcurrentSkipListSet<>();
        this.bus = bus;
    }

    void recycle(String tag) {
        cache.add(tag);
        if (!isStartRecycle) {
            startRecycle();
            isStartRecycle = true;
        }
    }

    private void startRecycle() {
        Observable
                .timer(RECYCLE_SPACE_TIME, TimeUnit.MILLISECONDS)
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        isStartRecycle = false;
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        for (String tag : cache) {
                            Subject<Object> subject = bus.get(tag);
                            if (subject != null && !subject.hasObservers()) {
                                bus.remove(tag);
                            }
                        }
                        cache.clear();
                    }
                });
    }
}