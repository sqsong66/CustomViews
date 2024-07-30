package com.sqsong.photoeditor.view.anno

import androidx.annotation.IntDef

@IntDef(TouchType.TOUCH_DOWN, TouchType.TOUCH_MOVE, TouchType.TOUCH_UP)
@Retention(AnnotationRetention.SOURCE)
annotation class TouchType {
    companion object {
        const val TOUCH_DOWN = 1
        const val TOUCH_MOVE = 2
        const val TOUCH_UP = 3
    }
}