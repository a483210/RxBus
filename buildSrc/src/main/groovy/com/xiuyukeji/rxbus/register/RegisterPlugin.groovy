package com.xiuyukeji.rxbus.register

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 自动查找注册Index
 *
 * @author Created by jz on 2018/5/8 17:52
 */
class RegisterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def isApp = project.plugins.hasPlugin(AppPlugin)
        if (isApp) {
            def android = project.extensions.getByType(AppExtension)
            def transform = new RegisterTransform(project)
            android.registerTransform(transform)
        }
    }

}
