package com.plcoding.androidstorage

import android.graphics.Bitmap

data class InternalStoragePhoto(
    val name: String,
    val bmp: Bitmap,
    val dateAdded: Long
)
