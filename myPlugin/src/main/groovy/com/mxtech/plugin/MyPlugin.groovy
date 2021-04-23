package com.mxtech.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project


public class MyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("Hello,This is my gradle plugin.")
        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        appExtension.registerTransform(new MyTransform())
    }
}