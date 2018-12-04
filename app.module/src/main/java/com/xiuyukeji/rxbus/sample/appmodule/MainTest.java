package com.xiuyukeji.rxbus.sample.appmodule;

import android.util.Log;

import com.xiuyukeji.rxbus.RxBus;
import com.xiuyukeji.rxbus.Subscribe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class MainTest implements LifecycleObserver {

    public MainTest(AppCompatActivity appCompatActivity) {
        appCompatActivity.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        RxBus.get().register(this);
    }

    @Subscribe(tag = "1")
    public void busText2(Long id) {
        Log.i("Tool", "id2 = " + id);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        RxBus.get().unregister(this);
    }
}
