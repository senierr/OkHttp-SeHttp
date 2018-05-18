package com.senierr.simple

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.senierr.permission.CheckCallback
import com.senierr.permission.PermissionManager
import com.senierr.sehttp.SeHttp
import com.senierr.sehttp.converter.FileConverter
import com.senierr.sehttp.converter.StringConverter
import com.senierr.sehttp.interceptor.LogLevel
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 *
 * @author zhouchunjie
 * @date 2018/5/17
 */
class MainActivity : AppCompatActivity() {

    private val logTag = MainActivity::class.java.name

    private lateinit var seHttp: SeHttp
    private var downloadDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 检查权限
        PermissionManager.with(this)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .request(object : CheckCallback() {
                    override fun onAllGranted() {
                        // 初始化界面
                        initView()
                        // 初始化SeHttp
                        seHttp = SeHttp.Builder()
                                .setDebug(logTag, LogLevel.BODY)
                                .setConnectTimeout(10 * 1000)
                                .setReadTimeout(10 * 1000)
                                .setWriteTimeout(10 * 1000)
                                .addCommonHeader("com_header", "com_header_value")
                                .addCommonUrlParam("com_url_param", "com_url_param_value")
                                .build()
                    }
                })
    }

    private fun initView() {
        btn_get.setOnClickListener { get() }
        btn_post.setOnClickListener { post() }
        btn_https.setOnClickListener { https() }
        btn_upload.setOnClickListener { upload() }
        btn_download.setOnClickListener { download() }
    }

    private fun get() {
        Single.fromCallable {
            return@fromCallable seHttp.get("http://www.baidu.com")
                    .addHeader("header", "header_value")
                    .addUrlParam("url_param", "url_param_value")
                    .executeWith(StringConverter())
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.e(logTag, "--success: $it")
                }, {
                    Log.e(logTag, "--onError: ${Log.getStackTraceString(it)}")
                })
    }

    private fun post() {

    }

    private fun https() {

    }

    private fun upload() {

    }

    private fun download() {
        if (downloadDisposable != null) {
            downloadDisposable?.dispose()
            downloadDisposable = null
            return
        }

        Observable.create<Int> {
            val destFile = File(Environment.getExternalStorageDirectory(), "WeChat.exe")
            seHttp.get(URL_DOWNLOAD)
                    .setOnDownloadListener { progress, _, _ ->
                        it.onNext(progress)
                    }
                    .executeWith(FileConverter(destFile))
            it.onComplete()
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    Log.e(logTag, "--doOnSubscribe: ${Thread.currentThread().name}")
                    btn_download.setText(R.string.cancel)
                }
                .doFinally {
                    Log.e(logTag, "--doFinally: ${Thread.currentThread().name}")
                    downloadDisposable = null
                    btn_download.setText(R.string.download)
                }
                .subscribe(object : Observer<Int> {
                    override fun onSubscribe(d: Disposable) {
                        Log.e(logTag, "--onSubscribe: ${Thread.currentThread().name}")
                        downloadDisposable = d
                    }

                    override fun onNext(t: Int) {
                        Log.e(logTag, "--onNext: $t")
                    }

                    override fun onComplete() {
                        Log.e(logTag, "--onComplete")
                    }

                    override fun onError(e: Throwable) {
                        Log.e(logTag, "--onError: ${Log.getStackTraceString(e)}")
                    }
                })
    }
}