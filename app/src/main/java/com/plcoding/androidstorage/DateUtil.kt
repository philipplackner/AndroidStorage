package com.plcoding.androidstorage

import java.text.SimpleDateFormat
import java.util.*

/**
 *  Rev           1.0
 *  Author        EmreHamurcu
 *  Date          6/23/2021
 *  FileName      DateUtil
 */
private const val DATE_FORMAT = "MM.dd.yyyy HH:mm:ss"

private const val DATE_FORMAT_TIME_STAMP = "dd.MM.yyyy"

fun String.getDateTime(): String {
    return try {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        //Android needs to different timestamp,weird but it should multiply by 1000
        val netDate = Date(this.toLong() * 1000)
        sdf.format(netDate)
    } catch (e: Exception) {
        ""
    }
}

fun dateToTimestamp(day: Int, month: Int, year: Int): Long {
    val result = SimpleDateFormat(DATE_FORMAT_TIME_STAMP, Locale.getDefault()).let { formatter ->
        formatter.parse("$day.$month.$year")?.time ?: 1000
    }
    //Android timestamp has 3 zeros additional, so we need to divide by 1000 to get rid off
    return result / 1000
}
