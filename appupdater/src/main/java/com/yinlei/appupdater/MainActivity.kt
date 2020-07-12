package com.yinlei.appupdater

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.yinlei.appupdater.updater.AppUpdater
import com.yinlei.appupdater.updater.bean.DownloadBean
import com.yinlei.appupdater.updater.network.INetCallback
import com.yinlei.appupdater.updater.network.INetDownloadCallback
import com.yinlei.appupdater.updater.ui.UpdateVersionShowDialog
import com.yinlei.appupdater.updater.utils.AppUtils
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
                    val versionCode = bean?.versionCode?.toLong()
                    if (versionCode!! <= AppUtils.getLocalVersionCode(this@MainActivity)) {
                        Toast.makeText(this@MainActivity, "当前app已经是最新版本,不需要更新!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    //如果需要更新
                    // 弹框
                    UpdateVersionShowDialog.show(this@MainActivity, bean)
                }

                override fun failed(throwable: Throwable) {
                    throwable.printStackTrace()
                    Toast.makeText(this@MainActivity, "版本更新接口请求失败!", Toast.LENGTH_SHORT).show()
                }
            }, this@MainActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUpdater.getInstance().getNetManager().cancel(this@MainActivity)
    }
}
