package com.yinlei.appupdater

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.yinlei.appupdater.updater.AppUpdater
import com.yinlei.appupdater.updater.bean.DownloadBean
import com.yinlei.appupdater.updater.network.INetCallback
import com.yinlei.appupdater.updater.network.INetDownloadCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val TAG = "yinlei"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpdater.setOnClickListener {
            AppUpdater.getInstance().getNetManager().get("http://59.110.162.30/app_updater_version.json", object : INetCallback {
                override fun success(response: String) {
                    Log.d(TAG, "response = $response")
                    // 解析json
                    val bean = DownloadBean.parse(response)
                    // 版本匹配
                    //如果需要更新
                    // 弹框
                    // 点击下载
                    val targetFile = File(cacheDir, "target.apk")
                    bean?.url?.let { url ->
                        AppUpdater.getInstance().getNetManager().download(
                            url, targetFile,
                            object : INetDownloadCallback {
                                override fun success(apkFile: File) {
                                    // 安装apk
                                    Log.d(TAG, "success = ${apkFile.absolutePath}")

                                }

                                override fun progress(progress: Int) {
                                    // 更新ui界面
                                    Log.d(TAG, "progress = $progress")
                                }

                                override fun failed(throwable: Throwable) {
                                    // 下载失败
                                    Toast.makeText(this@MainActivity, "文件下载失败!", Toast.LENGTH_SHORT).show()
                                }
                            } )
                    }
                }

                override fun failed(throwable: Throwable) {
                    throwable.printStackTrace()
                    Toast.makeText(this@MainActivity, "版本更新接口请求失败!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
