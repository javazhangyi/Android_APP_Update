package com.yinlei.appupdater.updater.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class AppUtils {

    companion object {
        // 得到本地的app的versioncode
        fun getLocalVersionCode(context: Context): Long {
            try {
                val packageManager = context.packageManager
                val packageInfo =packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    return packageInfo.longVersionCode
                } else {
                    return packageInfo.versionCode.toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return -1L
        }

        // 安装apk: 交给android系统安装程序去安装我们的apk
        // N: FileProvider适配(本质还是通过contentprovider对外分享uri)
        // O: INSTALL PERMISSION适配
        fun installApk(activity: Activity, apkFile: File) {
            var uri: Uri = Uri.fromFile(apkFile)

            val intent = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                action = Intent.ACTION_VIEW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(activity, activity.packageName+".fileprovider",apkFile)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            activity.startActivity(intent)
        }

        // 文件md5校验
        fun getFileMd5(targetFile: File) : String{
            if (!targetFile.isFile) {
                return ""
            }
            val digest: MessageDigest
            var istr: FileInputStream? = null
            val buffer = ByteArray(1024)
            var len = 0
            try {
                digest = MessageDigest.getInstance("MD5")
                istr = FileInputStream(targetFile)
                while (((istr.read(buffer)).also {
                        len = it
                    })!=-1) {
                    digest.update(buffer, 0, len)
                }
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                return ""
            } finally {
                istr?.let {
                    istr.close()
                }
            }

            val result = digest.digest()
            val bigInt = BigInteger(1, result)
            return bigInt.toString(16)
        }
    }
}