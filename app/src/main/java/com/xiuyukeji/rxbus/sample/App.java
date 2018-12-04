package com.xiuyukeji.rxbus.sample;

import androidx.multidex.MultiDexApplication;
import android.util.Log;

import com.xiuyukeji.rxbus.RxBus;

/**
 * Created by jinzhao on 2018/4/13.
 */
public class App extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        long st = System.currentTimeMillis();

        RxBus.get().registerIndex(this);

        Log.i("Tool", System.currentTimeMillis() - st + " ms");
    }
}
