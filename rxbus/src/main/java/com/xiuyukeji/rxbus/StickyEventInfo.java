package com.xiuyukeji.rxbus;

/**
 * 存储sticky事件信息
 *
 * @author Created by jz on 2016/12/27 16:36
 */
class StickyEventInfo {
    final boolean isSingle;
    final Object event;

    StickyEventInfo(Object event) {
        this(false, event);
    }

    StickyEventInfo(boolean isSingle, Object event) {
        this.isSingle = isSingle;
        this.event = event;
    }
}
