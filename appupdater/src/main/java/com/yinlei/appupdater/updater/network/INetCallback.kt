package com.yinlei.appupdater.updater.network

interface INetCallback {

    fun success(response: String)

    fun failed(throwable: Throwable)
}