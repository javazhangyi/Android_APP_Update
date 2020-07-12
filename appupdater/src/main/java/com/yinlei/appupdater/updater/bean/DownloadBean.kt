package com.yinlei.appupdater.updater.bean

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

data class DownloadBean(val title: String, val content: String, val url: String,val md5: String, val versionCode: String ): Serializable{

    companion object {
        fun parse(response: String): DownloadBean? {
            try {
                val repjson = JSONObject(response)
                val title = repjson.optString("title")
                val content = repjson.optString("content")
                val url = repjson.optString("url")
                val md5 = repjson.optString("md5")
                val versionCode = repjson.optString("versionCode")
                val bean = DownloadBean(title, content, url, md5, versionCode)
                return bean
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return null
        }
    }

}