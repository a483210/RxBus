package com.xiuyukeji.rxbus.lifecycle;

import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.trello.rxlifecycle2.LifecycleTransformer;
import com.trello.rxlifecycle2.RxLifecycle;

import io.reactivex.subjects.PublishSubject;

/**
 * RxLifecycle帮助类，基础类
 *
 * @author Created by jz on 2016/12/30 15:23
 */
public class BaseRxLifecycleHelper<E> {

    private final PublishSubject<E> mLifecycleSubject;

    public BaseRxLifecycleHelper() {
        this.mLifecycleSubject = PublishSubject.create();
    }

    @NonNull
    @CheckResult
    @CallSuper
    public <T> LifecycleTransformer<T> bindUntilEvent(@NonNull E event) {
        return RxLifecycle.bindUntilEvent(mLifecycleSubject, event);
    }

    /**
     * 开始取消订阅
     *
     * @param event 事件
     */
    @CallSuper
    public void unbindEvent(@NonNull E event) {
        mLifecycleSubject.onNext(event);
    }
}
