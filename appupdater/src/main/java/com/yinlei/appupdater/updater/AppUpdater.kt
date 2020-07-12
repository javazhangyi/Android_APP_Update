package com.yinlei.appupdater.updater

import com.yinlei.appupdater.updater.network.INetManager
import com.yinlei.appupdater.updater.network.OkhttpNetworkManager

class AppUpdater {

    // 需要网络请求、下载
    private var mNetManager: INetManager = OkhttpNetworkManager()

    // 对外暴露想要用哪种netmanager(接口隔离具体的实现)
    fun setNetManager(manager: INetManager) {
        mNetManager = manager
    }

    fun getNetManager(): INetManager  = mNetManager


    companion object {
        private val mInstance = AppUpdater()
        fun getInstance(): AppUpdater = mInstance
    }
}