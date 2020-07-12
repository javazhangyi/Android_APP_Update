package com.yinlei.appupdater.updater.network

import java.io.File


interface INetManager {

    // 网络请求
    fun get(url: String, callback: INetCallback, tag: Any)

    // 下载文件apk
    fun download(url: String, targetFile: File?, callback: INetDownloadCallback, tag: Any)

    // 中断下载和请求
    fun cancel(tag: Any)

}