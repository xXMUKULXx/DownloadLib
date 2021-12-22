package com.example.scopestoragelib

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DataDownloader(
    private val context: Context,
) {

    data class FileData(
        var file: File,
        var fileName: String,
        var filePath: String,
        var fileUri: Uri,
    )

    interface DataDownloaderCallbacks {
        fun progress(progressPercentage: Int)
        fun onSuccess(fileData: FileData)
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

                val filePath = file.absolutePath
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "com.scopestoragelib.provider",
                    file
                )

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
                    callback.onSuccess(FileData(file, fileName, filePath, fileUri))
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
        var file: File
        val fileName = "$filename.$extension"
        var filePath: String
        var fileUri: Uri

        myExecutor.execute {
            try {
                val url = URL(srcUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val contentLength = connection.contentLength
                val buffer = ByteArray(contentLength)

                sdkAbove10 {
                    val desDirectory = "${Environment.DIRECTORY_DOWNLOADS}/$outputDir"
                    file = File(desDirectory)
                    if (!file.exists()) {
                        file.mkdir()
                    }
                    filePath = file.absolutePath
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
                    uri?.let { it ->
                        fileUri = it
                        outputStream = context.contentResolver.openOutputStream(it)
                        if (outputStream != null) {
                            val bos = BufferedOutputStream(outputStream)
                            var total: Long = 0
                            var count: Int
                            while (bis.read(buffer).also { count = it } != -1) {
                                total += count.toLong()
                                myHandler.post {
                                    callback.progress((total * 100 / contentLength).toInt())
                                }
                                bos.write(buffer, 0, count)
                                bos.flush()
                            }
                            bos.close()
                        }
                        myHandler.post {
                            callback.onSuccess(FileData(file, fileName, filePath, fileUri))
                        }
                    } ?: run {
                        throw NullPointerException("fileUri is null.")
                    }
                    bis.close()
                } ?: run {
                    val desDirector =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator + outputDir
                    val folder = File(desDirector)
                    if (!folder.exists()) folder.mkdirs()
                    file = File(folder, fileName)
                    if (!file.exists()) file.createNewFile()
                    filePath = file.absolutePath
                    fileUri = file.toUri()
                    val stream = DataInputStream(url.openStream())
                    val fis = FileOutputStream(file.path)
                    val fos = DataOutputStream(fis)
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
                    fis.close()
                    myHandler.post {
                        callback.onSuccess(FileData(file, fileName, filePath, fileUri))
                    }
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