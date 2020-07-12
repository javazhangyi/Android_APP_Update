package com.yinlei.appupdater.updater.ui

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.yinlei.appupdater.R
import com.yinlei.appupdater.updater.AppUpdater
import com.yinlei.appupdater.updater.bean.DownloadBean
import com.yinlei.appupdater.updater.network.INetDownloadCallback
import com.yinlei.appupdater.updater.utils.AppUtils
import java.io.File

// 弹框
class UpdateVersionShowDialog: DialogFragment() {
    private val TAG = "yinlei"

    private lateinit var downloadBean: DownloadBean
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            downloadBean = it.getSerializable(KEy_DOWNLOAD_BEAN) as DownloadBean
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_updater, container, false)
        bindEvents(view)
        return view
    }

    private fun bindEvents(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvContent = view.findViewById<TextView>(R.id.tvContent)
        val tvUpdate = view.findViewById<TextView>(R.id.tvUpdate)
        tvTitle.setText(downloadBean.title)
        tvContent.setText(downloadBean.content)
        tvUpdate.setOnClickListener {
            it.isEnabled = false
            // 点击下载(弹框里做)
            val targetFile = File(activity?.cacheDir, "target.apk")
            downloadBean?.url?.let { url ->
                AppUpdater.getInstance().getNetManager().download(
                    url, targetFile,
                    object : INetDownloadCallback {
                        override fun success(apkFile: File) {
                            it.isEnabled = true
                            // 安装apk
                            Log.d(TAG, "success = ${apkFile.absolutePath}")
                            dismiss()
                            val fileMd5 = AppUtils.getFileMd5(apkFile)
                            Log.d(TAG, "md5 = ${fileMd5}")
                            if (fileMd5 == downloadBean.md5) {
                                AppUtils.installApk(requireActivity(), apkFile)
                            } else {
                                Toast.makeText(activity, "md5校验失败,该文件不完整or被篡改过!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun progress(progress: Int) {
                            // 更新ui界面
                            Log.d(TAG, "progress = $progress")
                            tvUpdate.text = "${progress}%"
                        }

                        override fun failed(throwable: Throwable) {
                            it.isEnabled = true
                            // 下载失败
                            Toast.makeText(activity, "文件下载失败!", Toast.LENGTH_SHORT).show()
                        }
                    }, this@UpdateVersionShowDialog)
            }
        }
    }

    // 这里采用的是自绘secene，也可以启用其他方式进行绘制弹框的背景
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))// 透明色

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        AppUpdater.getInstance().getNetManager().cancel(this@UpdateVersionShowDialog)
    }

    companion object {

        private val KEy_DOWNLOAD_BEAN = "download_bean"

        fun show(activity: FragmentActivity, bean: DownloadBean) {
            val bundle = Bundle()
            bundle.putSerializable(KEy_DOWNLOAD_BEAN, bean)
            val dialog = UpdateVersionShowDialog()
            dialog.arguments = bundle
            dialog.show(activity.supportFragmentManager, "updateVersionShowDialog")
        }
    }
}