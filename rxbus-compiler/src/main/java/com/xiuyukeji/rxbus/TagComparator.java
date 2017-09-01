package com.xiuyukeji.rxbus;

import java.util.Comparator;

/**
 * Tag排序规则
 *
 * @author Created by jz on 2016/12/7 14:41
 */
class TagComparator implements Comparator<ProcessorInfo> {
    @Override
    public int compare(ProcessorInfo lhs, ProcessorInfo rhs) {
        if (lhs.tag > rhs.tag) {
            return 1;
        } else if (lhs.tag < rhs.tag) {
            return -1;
        }
        return 0;
    }
}