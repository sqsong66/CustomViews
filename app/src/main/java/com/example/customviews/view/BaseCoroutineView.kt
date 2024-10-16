package com.example.customviews.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

open class BaseCoroutineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), CoroutineScope {

    private val coroutineJob by lazy { Job() }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main.immediate + coroutineJob

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineJob.cancel()
    }
}