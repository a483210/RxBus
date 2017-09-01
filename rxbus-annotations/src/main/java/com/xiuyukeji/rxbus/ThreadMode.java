package com.xiuyukeji.rxbus;

/**
 * 线程类型
 *
 * @author Created by jz on 2016/12/7 10:16
 */
public enum ThreadMode {
    /**
     * current thread
     */
    POST,
    /**
     * android main thread
     */
    MAIN,

    /**
     * new thread
     */
    ASYNC,
    /**
     * io
     */
    IO,
    /**
     * single
     */
    SINGLE,
}