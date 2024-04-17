package com.example.customviews.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class InfiniteScrollTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt() // 文字颜色
        textSize = 40f // 文字大小
    }

    private var text = "这是无限滚动的文字，滑动屏幕查看更多内容。"
    private var textWidth = textPaint.measureText(text)
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.RED)

        val screenWidth = width
        val lineHeight = textPaint.textSize * 1.5f

        var x = offsetX % textWidth
        if (x > 0) x -= textWidth

        var y = offsetY % lineHeight
        if (y > 0) y -= lineHeight

        while (y < height) {
            var tempX = x
            while (tempX < screenWidth) {
                canvas.drawText(text, tempX, y + textPaint.textSize, textPaint)
                tempX += textWidth
            }
            y += lineHeight
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录触摸点坐标
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                // 计算移动的距离
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                offsetX += dx
                offsetY += dy

                invalidate() // 重绘视图

                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }

    // 用于记录上次触摸事件的X，Y坐标
    private var lastTouchX = 0f
    private var lastTouchY = 0f
}
