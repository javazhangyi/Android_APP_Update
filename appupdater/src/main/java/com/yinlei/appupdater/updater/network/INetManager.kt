package com.yinlei.appupdater.updater.network

import java.io.File


interface INetManager {

    // 网络请求
    fun get(url: String, callback: INetCallback)

    // 下载文件apk
    fun download(url: String, targetFile: File?, callback: INetDownloadCallback)

}