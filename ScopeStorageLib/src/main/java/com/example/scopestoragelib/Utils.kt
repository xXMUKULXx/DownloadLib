package com.example.scopestoragelib

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R, lambda = 0)
internal inline fun <T> sdkAbove10(callback: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        callback()
    } else {
        null
    }
}