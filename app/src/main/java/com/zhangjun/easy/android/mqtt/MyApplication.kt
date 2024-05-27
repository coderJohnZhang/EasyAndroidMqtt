package com.zhangjun.easy.android.mqtt

import android.app.Application

/**
 * Created by ZhangJun on 2018/10/15.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        private val TAG = MyApplication::class.java.simpleName
        lateinit var instance: MyApplication
            private set
    }
}
