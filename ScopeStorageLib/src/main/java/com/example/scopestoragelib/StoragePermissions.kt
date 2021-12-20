package com.example.scopestoragelib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

class StoragePermissions(
    private val activity: Activity
) {

    private val permissions: Array<String> = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun request(permissionIntent: (arr: Array<String>) -> Unit) {
        if (!check()) {
            sdkAbove10 {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format(activity.packageName))
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    activity.startActivity(intent)
                }
            } ?: run {
                permissionIntent(permissions)
            }
        }
    }

    fun check(): Boolean {
        return sdkAbove10 {
            // for android 11 and above
            Environment.isExternalStorageManager()
        } ?: run {
            // for android 10 and below
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}