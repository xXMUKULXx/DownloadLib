package com.example.scopestoragelib

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DataDownloader(
    private val context: Context,
) {

    interface DataDownloaderCallbacks {
        fun progress(progressPercentage: Int)
        fun onSuccess(path: String)
        fun onFailure(error: String)
    }

    //    Download Url data inside in-app directories.
    fun downloadInInternalStorage(
        url: String,
        outputDir: String,
        filename: String,
        extension: String,
        callback: DataDownloaderCallbacks
    ) {

        val myHandler = Handler(Looper.getMainLooper())
        val myExecutor = Executors.newSingleThreadExecutor()

        myExecutor.execute {
            try {
                val folder =
                    File("${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/$outputDir")
                if (!folder.exists()) folder.mkdirs()
                val fileName = "$filename.$extension"
                val file = File(folder, fileName)
                if (!file.exists()) file.createNewFile()
                val u = URL(url)
                val conn = u.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val contentLength: Int = conn.contentLength
                val stream = DataInputStream(u.openStream())
                val buffer = ByteArray(contentLength)
                val fos = DataOutputStream(FileOutputStream(file))
                var total: Long = 0
                var count: Int
                while (stream.read(buffer).also { count = it } != -1) {
                    total += count
                    if (contentLength > 0)
                        myHandler.post {
                            callback.progress((total * 100 / contentLength).toInt())
                        }
                    fos.write(buffer, 0, count)
                }
                stream.close()
                fos.flush()
                fos.close()
                myHandler.post {
                    callback.onSuccess(file.absolutePath)
                }
            } catch (e: Exception) {
                myHandler.post {
                    callback.onFailure(e.message.toString())
                }
            }
        }
    }

    //    Download Url data in external downloads directory.
    fun downloadInExternalStorage(
        srcUrl: String,
        outputDir: String,
        filename: String,
        extension: String,
        callback: DataDownloaderCallbacks
    ) {
        val myHandler = Handler(Looper.getMainLooper())
        val myExecutor = Executors.newSingleThreadExecutor()
        var filePath: String? = null

        myExecutor.execute {
            try {
                val url = URL(srcUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val contentLength = connection.contentLength
                val buffer = ByteArray(contentLength)
                val fileName = "$filename.$extension"
                sdkAbove10 {
                    val desDirectory = "${Environment.DIRECTORY_DOWNLOADS}/$outputDir"
                    val desFile = File(desDirectory)
                    if (!desFile.exists()) {
                        desFile.mkdir()
                    }
                    filePath = desFile.absolutePath
                    val values = ContentValues()
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, desDirectory)
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )
                    val inputStream = connection.inputStream
                    val bis = BufferedInputStream(inputStream)
                    val outputStream: OutputStream?
                    if (uri != null) {
                        outputStream = context.contentResolver.openOutputStream(uri)
                        if (outputStream != null) {
                            val bos = BufferedOutputStream(outputStream)
                            var bytes = bis.read(buffer)
                            var total: Long = 0
                            var count: Int
                            while (bytes.also { count = it } != -1) {
                                total += count.toLong()
                                myHandler.post {
                                    callback.progress((total * 100 / contentLength).toInt())
                                }
                                bos.write(buffer, 0, count)
                                bos.flush()
                                bytes = bis.read(buffer)
                            }
                            bos.close()
                        }
                    }
                    bis.close()
                } ?: run {
                    val desDirector =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator + outputDir
                    val folder = File(desDirector)
                    if (!folder.exists()) folder.mkdirs()
                    val desFile = File(folder, fileName)
                    if (!desFile.exists()) desFile.createNewFile()
                    filePath = desFile.absolutePath
                    val stream = DataInputStream(url.openStream())
                    val fos = DataOutputStream(FileOutputStream(desFile.path))
                    var total: Long = 0
                    var count: Int
                    while (stream.read(buffer).also { count = it } != -1) {
                        total += count
                        if (contentLength > 0)
                            myHandler.post {
                                callback.progress((total * 100 / contentLength).toInt())
                            }
                        fos.write(buffer, 0, count)
                    }
                    stream.close()
                    fos.flush()
                    fos.close()
                }
                myHandler.post {
                    callback.onSuccess(filePath.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                myHandler.post {
                    callback.onFailure(e.message.toString())
                }
            }
        }
    }
}