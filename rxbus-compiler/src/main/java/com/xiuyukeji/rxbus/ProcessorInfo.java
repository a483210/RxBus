package com.xiuyukeji.rxbus;

import com.squareup.javapoet.ClassName;

/**
 * 记录处理信息
 *
 * @author Created by jz on 2016/12/8 18:17
 */
class ProcessorInfo {
    final int tag;
    final boolean sticky;
    final ThreadMode mode;
    final ClassName parameterTypeName;
    final String methodName;

    ProcessorInfo(int tag, boolean sticky, ThreadMode mode, ClassName parameterTypeName, String methodName) {
        this.tag = tag;
        this.sticky = sticky;
        this.mode = mode;
        this.parameterTypeName = parameterTypeName;
        this.methodName = methodName;
    }
}
