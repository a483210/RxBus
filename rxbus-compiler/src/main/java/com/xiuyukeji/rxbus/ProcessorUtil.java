package com.xiuyukeji.rxbus;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * 工具类
 *
 * @author Created by jz on 2017/8/24 17:32
 */
class ProcessorUtil {
    private ProcessorUtil() {
    }

    static TypeMirror getParamTypeMirror(VariableElement param) {
        TypeMirror typeMirror = param.asType();
        if (typeMirror instanceof TypeVariable) {
            return ((TypeVariable) typeMirror).getUpperBound();
        }
        return typeMirror;
    }
}
