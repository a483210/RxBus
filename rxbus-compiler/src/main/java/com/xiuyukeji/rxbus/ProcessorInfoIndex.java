package com.xiuyukeji.rxbus;

import com.squareup.javapoet.ClassName;

import java.util.ArrayList;

/**
 * 记录处理信息序列
 *
 * @author Created by jz on 2016/12/8 18:17
 */
class ProcessorInfoIndex {
    final ClassName classTypeName;
    final ArrayList<ProcessorInfo> processorInfos;

    ProcessorInfoIndex(ClassName classTypeName) {
        this.classTypeName = classTypeName;
        this.processorInfos = new ArrayList<>();
    }
}
