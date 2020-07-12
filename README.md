# APP应用内升级(Android)


### 1. 为什么需要应用内升级?

- apk上架市场周期慢，无法回退
- 可以小规模实验以及试错(新功能实验，稳定性检测)
- 可以快速收敛版本(新功能覆盖，严重bug修复)

### 2. app中应用内升级的几种形式：

- 应用启动时静默检测，提示更新
- 用户手动在设置页，点击检测更新

### 3.app应用内升级的实现流程：

网络请求->返回结果->是否需要更新->弹框显示版本说明->确定下载->下载并通知用户进度->完整性(md5)检测->安装

------

### 网络模块的设计：

- 考虑通过接口隔离具体实现
- 使用okhttp完成接口实现,实现get请求，文件下载

### UI实现：

- 通过DialogFragment而不是直接使用Dialog
- 接入网络请求，进度回调

### 安装apk和一些细节处理:

- 用户下载过程中cancel，如何及时的取消请求，中断下载
- apk的完整性校验
- 为了方便演示，使用应用内部的cache文件夹，避免涉及到存储卡权限(Android Q限制)

### 适配：

- 应用内安装，涉及到文件uri的传递，需要进行适配(Android N FileProvider适配)
- 引入安装权限(Android O对应用安装进行的权限的检测)
- P上，默认不允许直接使用http的请求，需要更安全的https【清单文件中也可以配置直接使用明文传输】(Android P对http网络请求的约束)

### 已完成的功能：

- [x] 网络框架搭建
- [x] 文件的请求和下载
- [x] APP的安装
- [x] MD5文件校验
- [ ] 断点续传、续下(分段多线程下载apk) [需要1.http的Range属性支持：下载一个文件的起始字节和终止字节]、2.本地合并分段下载的文件(RandomAccessFile的seek())
- [ ] 增量更新(old apk DIFF new apk -> gen a patch->download patch -> old apk merge patch then become new apk)【bsdiff算法】
- [ ] 对于自签名https接口需要okhttp证书配置sslfactory

### 运行效果图：

![screen1](.\Screenshot\screen1.png)

![screen2](.\Screenshot\screen2.png)

![screen3](.\Screenshot\screen3.png)

------

## 功能代码：

1. 检查当前app的versioncode等信息:

   ```kotlin
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
   ```

   

   2.app的安装：涉及fileprovider的适配、android O安装package权限申请:



 

```kotlin
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
```

关于fileprovider需要进行清单文件的注册：

```xml
<provider
    android:authorities="${applicationId}.fileprovider"
    android:name="androidx.core.content.FileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/fileproviderpath"/>
</provider>
```

> 其中name是系统的不需要我们创建，只需要创建resource="@xml/fileproviderpath"



res下的xml文件夹下的fileproviderpath.xml文件:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
<!--    file path传递给别人不安全，所以需要转换为content uri系统希望 name可以随便写-->
    <root-path name="root" path=".">
    </root-path>

    <files-path
        name="files"
        path=".">
    </files-path>

    <cache-path
        name="cache"
        path=".">
    </cache-path>

    <external-path
        name="external"
        path=".">
    </external-path>

    <external-cache-path
        name="external_cache"
        path=".">
    </external-cache-path>

    <external-files-path
        name="external_file"
        path=".">
    </external-files-path>
</paths>
```
权限的申请：    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>

具体的详细配置和解释请查阅Google官方文档

3. MD5文件校验：下载的文件是否被篡改过or下载的文件是否是完整的

   ```kotlin
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
   ```

4. 正确中断网络请求：

```kotlin
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
```

记得一定在请求的时候加上tag:

```kotlin
val request = builder.url(url).get().tag(tag).build()
```

取消请求：

```kotlin
AppUpdater.getInstance().getNetManager().cancel(this@UpdateVersionShowDialog)
```

this@UpdateVersionShowDialog根据具体实例而定





