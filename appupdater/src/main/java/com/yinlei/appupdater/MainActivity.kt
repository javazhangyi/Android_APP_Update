package com.yinlei.appupdater

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.yinlei.appupdater.updater.AppUpdater
import com.yinlei.appupdater.updater.network.INetCallback
import com.yinlei.appupdater.updater.network.INetDownloadCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpdater.setOnClickListener {
            AppUpdater.getInstance().getNetManager().get("", object : INetCallback {
                override fun success(response: String) {
                    // 解析json
                    // 版本匹配
                    //如果需要更新
                    // 弹框
                    // 点击下载
                    AppUpdater.getInstance().getNetManager().download("", File("hello.txt"),
                        object : INetDownloadCallback {
                            override fun success(apkFile: File) {
                                // 安装apk
                            }

                            override fun progress(progress: Int) {
                                // 更新ui界面
                            }

                            override fun failed(throwable: Throwable) {
                                // 下载失败
                            }
                        } )
                }

                override fun failed(throwable: Throwable) {
                    Toast.makeText(this@MainActivity, "版本更新接口请求失败!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
