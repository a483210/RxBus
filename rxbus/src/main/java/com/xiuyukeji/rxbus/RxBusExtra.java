package com.xiuyukeji.rxbus;

import com.xiuyukeji.rxbus.lifecycle.IntRxLifecycleHelper;
import com.xiuyukeji.rxbus.lifecycle.StringRxLifecycleHelper;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Functions;

import static com.xiuyukeji.rxbus.utils.SubscriberUtils.applySchedulers;

/**
 * RxBus额外的拓展
 *
 * @author Created by jz on 2016/12/14 9:27
 */

public class RxBusExtra {

    private static volatile RxBusExtra instance;

    public static RxBusExtra get() {
        if (instance == null) {
            synchronized (RxBusExtra.class) {
                if (instance == null) {
                    instance = new RxBusExtra();
                }
            }
        }
        return instance;
    }

    private int sequence;
    private final IntRxLifecycleHelper lifecycleHelper;
    private final StringRxLifecycleHelper tagLifecycleHelper;

    private RxBusExtra() {
        this.lifecycleHelper = new IntRxLifecycleHelper();
        this.tagLifecycleHelper = new StringRxLifecycleHelper();
    }

    @CheckResult
    public <T> Observable<T> take(@NonNull Class<T> eventType) {
        return take(eventType, EventType.DEFAULT_TAG);
    }

    @CheckResult
    public <T> Observable<T> take(@NonNull Class<T> eventType, String tag) {
        return take(eventType, tag, ThreadMode.POST);
    }

    /**
     * 注册事件，如果使用该方法需要自己解除订阅
     *
     * @param eventType 类型
     * @param tag       标识
     * @param mode      线程模式
     * @return observable
     */
    @CheckResult
    public <T> Observable<T> take(@NonNull Class<T> eventType, String tag, @NonNull ThreadMode mode) {
        return obtainSubject(tag)
                .compose(applySchedulers(mode))
                .ofType(eventType);
    }

    @CheckResult
    public <T> Observable<T> single(@NonNull Class<T> eventType) {
        return single(eventType, EventType.DEFAULT_TAG);
    }

    @CheckResult
    public <T> Observable<T> single(@NonNull Class<T> eventType, String tag) {
        return single(eventType, tag, ThreadMode.POST);
    }

    /**
     * 只会执行一次的事件，在被执行后会自动解除订阅
     *
     * @param eventType 类型
     * @param tag       标识
     * @param mode      线程模式
     */
    @CheckResult
    public <T> Observable<T> single(@NonNull Class<T> eventType, String tag, @NonNull ThreadMode mode) {
        final int sequence = getSequence();
        return obtainSubject(tag)
                .compose(applySchedulers(mode))
                .filter(Functions.isInstanceOf(eventType))
                .compose(lifecycleHelper.bindUntilEvent(sequence))
                .cast(eventType)
                .doOnNext(new Consumer<T>() {
                    @Override
                    public void accept(T t) throws Exception {
                        lifecycleHelper.unbindEvent(sequence);
                    }
                });
    }

    /**
     * 解除tag所有的订阅
     *
     * @param tag 标识
     */
    public void unsubscriber(String tag) {
        tagLifecycleHelper.unbindEvent(tag);
    }

    private Observable<Object> obtainSubject(String tag) {
        return RxBus.get()
                .eventHelper
                .obtainSubject(tag)
                .compose(tagLifecycleHelper.bindUntilEvent(tag));
    }

    private int getSequence() {
        return sequence++;
    }
}
