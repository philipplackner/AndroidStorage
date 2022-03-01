package com.plcoding.androidstorage

import android.os.Build

/**
 * Created By Dhruv Limbachiya on 01-03-2022 11:46 AM.
 */

inline fun <T> isSDK29AndUp(onSdk29: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29()
    } else null
}