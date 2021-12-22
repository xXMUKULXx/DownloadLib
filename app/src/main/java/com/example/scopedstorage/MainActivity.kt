package com.example.scopedstorage

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.scopestoragelib.DataDownloader
import com.example.scopestoragelib.StoragePermissions
import kotlinx.android.synthetic.main.activity_main.*


@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MAIN_ACTIVITY"
        const val url: String = "https://flyermaker.vasundharaapps.com/public/image/xr_2.mp4"
        const val outputDir: String = "ScopedStorage"
        const val filename: String = "video"
        const val extension: String = "mp4"
    }

    private var lastTask: String = ""

    private var permissionResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (StoragePermissions(this).check()) {
                tvDisplay.text = "permission granted"
                Log.e(TAG, "result: true")
                when (lastTask) {
                    "external" -> {
                        downloadExternal()
                    }
                    "internal" -> {
                        downloadInternal()
                    }
                }
            } else {
                tvDisplay.text = "permission rejected"
            }
        }

    private var downloader: DataDownloader = DataDownloader(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        btnDownloadExternal.setOnClickListener {
            lastTask = "external"
            if (StoragePermissions(this).check()) {
                downloadExternal()
            } else {
                StoragePermissions(this).request { permissionResult.launch(it) }
            }
        }
        btnDownloadInternal.setOnClickListener {
            lastTask = "internal"
            if (StoragePermissions(this).check()) {
                downloadInternal()
            } else {
                StoragePermissions(this).request { permissionResult.launch(it) }
            }
        }
    }

    private fun downloadExternal() {
        tvDisplay.text = "starting download..."
        downloader.downloadInExternalStorage(
            url, outputDir, filename, extension,
            object : DataDownloader.DataDownloaderCallbacks {
                override fun progress(progressPercentage: Int) {
                    tvDisplay.text = "progress: $progressPercentage %"
                }

                override fun onSuccess(fileData: DataDownloader.FileData) {
                    tvDisplay.text = "successfully saved at $fileData"
                }

                override fun onFailure(error: String) {
                    tvDisplay.text = "Error: $error"
                }
            })
    }

    private fun downloadInternal() {
        tvDisplay.text = "starting download..."
        downloader.downloadInInternalStorage(
            url, outputDir, filename, extension,
            object : DataDownloader.DataDownloaderCallbacks {
                override fun progress(progressPercentage: Int) {
                    tvDisplay.text = "progress: $progressPercentage %"
                }

                override fun onSuccess(fileData: DataDownloader.FileData) {
                    tvDisplay.text = "successfully saved at ${fileData.filePath}"
                }

                override fun onFailure(error: String) {
                    tvDisplay.text = "Error: $error"
                }
            })
    }
}