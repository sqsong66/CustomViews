package com.sqsong.opengllib.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.WindowManager

fun getDisplayRefreshNsec(activity: Activity): Long {
    val display = (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val displayFps = display.refreshRate.toDouble()
    val refreshNs = Math.round(1000000000L / displayFps)
    Log.d("songmao", "refresh rate is $displayFps fps --> $refreshNs ns")
    return refreshNs
}

