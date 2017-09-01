package com.xiuyukeji.rxbus.utils;

import java.util.Arrays;

/**
 * 缓存存储，初始size值决定效率
 *
 * @param <E>
 * @author Created by jz on 2016/12/21 15:59
 */
public class CacheList<E> {
    private final int defaultSize;
    private final E[] defaultData;
    private E[] data;
    private int size;

    public CacheList(E[] data) {
        this.defaultSize = data.length;
        this.defaultData = data;
        this.data = defaultData;
    }

    public void add(E e) {
        if (size >= defaultSize) {
            data = Arrays.copyOf(data, size + 1);
        }
        data[size++] = e;
    }

    public E get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return data[index];
    }

    public void clear() {
        int count = size > defaultSize ? defaultSize : size;
        for (int i = 0; i < count; i++) {
            defaultData[i] = null;
        }
        size = 0;
        data = defaultData;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}