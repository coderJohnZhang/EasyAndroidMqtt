package com.zhangjun.easy.android.mqtt

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by ZhangJun on 2018/07/14.
 */

class SharedPreferencesUtil private constructor() {

    private val sp: SharedPreferences = MyApplication.instance.getSharedPreferences(
        SHARE_PREFERENCE_FILE_NAME, Context.MODE_PRIVATE
    )
    private val editor: SharedPreferences.Editor? = sp.edit()

    /**
     * 返回所有的键值对
     *
     * @return
     */
    val all: Map<String, *>
        get() = sp.all

    fun setString(key: String, value: String) {
        editor!!.putString(key, value)
        editor.apply()
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getString(key: String, defaultString: String): String? {
        return sp.getString(key, defaultString)
    }

    fun setLong(key: String, value: Long) {
        editor!!.putLong(key, value)
        editor.apply()
    }

    fun getLong(key: String, defaultLong: Long): Long {
        return sp.getLong(key, defaultLong)
    }

    fun setInt(key: String, value: Int) {
        editor!!.putInt(key, value)
        editor.apply()
    }

    fun getInt(key: String, defaultInt: Int): Int {
        return sp.getInt(key, defaultInt)
    }

    fun setBoolean(key: String, value: Boolean) {
        editor!!.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String, defaultBoolean: Boolean): Boolean {
        return sp.getBoolean(key, defaultBoolean)
    }

    /**
     * 移除某个key值已经对应的值
     *
     * @param key
     */
    fun remove(key: String) {
        editor!!.remove(key)
        editor.commit()
    }

    /**
     * 清除所有的数据
     */
    fun clear() {
        editor?.clear()
    }

    /**
     * 查询某个key是否存在
     *
     * @param key
     * @return
     */
    operator fun contains(key: String): Boolean {
        return sp.contains(key)
    }

    companion object {

        private const val SHARE_PREFERENCE_FILE_NAME = "userInfo"

        val instance: SharedPreferencesUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SharedPreferencesUtil()
        }
    }
}
