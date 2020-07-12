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

    override fun get(url: String, callback: INetCallback) {
        val builder = Request.Builder()
        val request = builder.url(url).get().build()
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

    override fun download(url: String, targetFile: File?, callback: INetDownloadCallback) {
        if(!targetFile!!.exists()){
            targetFile.parentFile.mkdirs()
        }
        val builder = Request.Builder()
        val request = builder.url(url).get().build()
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
                        }) != -1){
                        os.write(buffer, 0, bufferLen)
                        os.flush()
                        curLen += bufferLen
                        sHandler.post {
                            callback.progress((curLen * 1.0f / totalLen!! * 100).toInt())
                        }
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
}