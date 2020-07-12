package com.yinlei.appupdater.updater.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkhttpNetworkManager: INetManager {

    companion object {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
        val sOkHttpClient: OkHttpClient = builder.build()
        val sHandler = Handler(Looper.getMainLooper())
    }

    override fun get(url: String, callback: INetCallback, tag: Any) {
        val builder = Request.Builder()
        val request = builder.url(url).get().tag(tag).build()
        val call = sOkHttpClient.newCall(request)
//        call.execute()
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 非ui线程
                sHandler.post{
                    callback.failed(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val str = response.body?.string()
                    sHandler.post{
                        callback.success(str!!)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    callback.failed(e)
                }
            }
        })
    }

    override fun download(
        url: String,
        targetFile: File?,
        callback: INetDownloadCallback,
        tag: Any
    ) {
        if(!targetFile!!.exists()){
            targetFile.parentFile.mkdirs()
        }
        val builder = Request.Builder()
        val request = builder.url(url).get().tag(tag).build()
        val call = sOkHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.failed(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val istr = response.body?.byteStream()
                val os = FileOutputStream(targetFile)
                try {// 文件的保存
                    val totalLen = response.body?.contentLength()
                    val buffer = ByteArray(8*1024) // 设置缓冲区
                    var curLen = 0L // 当前写了多少字节
                    var bufferLen = 0
                    while (((istr?.read(buffer)).also {
                            bufferLen = it!!
                        }) != -1 && !call.isCanceled()){
                        os.write(buffer, 0, bufferLen)
                        os.flush()
                        curLen += bufferLen
                        sHandler.post {
                            callback.progress((curLen * 1.0f / totalLen!! * 100).toInt())
                        }
                    }

                    if (call.isCanceled()) {
                        return
                    }

                    //我的文件需要暴露给系统安装器。属于2个不同的进程，所以有权限上的问题
                    try {
                        targetFile.setExecutable(true, false)// 不仅仅是拥有该文件者可以执行
                        targetFile.setReadable(true, false)
                        targetFile.setWritable(true, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    sHandler.post {
                        callback.success(targetFile!!)
                    }
                } catch (e: Throwable) {
                    if (call.isCanceled()) {
                        return
                    }
                    sHandler.post {
                        callback.failed(e)
                    }
                } finally {
                    istr?.let {
                        istr.close()
                    }
                    os.let {
                        os.close()
                    }
                }

            }
        })
    }

    // 中断下载和网络请求
    /**
     * 1. 取消正在排队等待网络请求的call（这里指 的是下载多任务）
     * 2. 正在执行任务的call
     */
    override fun cancel(tag: Any) {
        val queuedCalls = sOkHttpClient.dispatcher.queuedCalls() // 拿到网络请求排队的call
        for (call in queuedCalls) {
            if (tag == call.request().tag()) {
                call.cancel()
            }
        }
        val runningCalls = sOkHttpClient.dispatcher.runningCalls() // 拿到正在执行的call
        for (call in runningCalls) {
            if (tag == call.request().tag()) {
                call.cancel()
            }
        }
    }
}