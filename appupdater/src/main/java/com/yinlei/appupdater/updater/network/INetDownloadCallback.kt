package com.yinlei.appupdater.updater.network

import java.io.File

interface INetDownloadCallback {

    fun success(apkFile: File)

    fun progress(progress: Int)

    fun failed(throwable: Throwable)
}