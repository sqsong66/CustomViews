package com.example.customviews.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.NinePatch
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.NinePatchDrawable
import android.util.AttributeSet
import android.util.Log
import com.example.customviews.data.Solution
import com.example.customviews.utils.dp2Px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class PictureInfo(
    val imageRect: RectF,
    val imagePath: Path,
    val imageBitmap: Bitmap,
    val ninePatchDrawable: NinePatchDrawable?,
    val imageMatrix: Matrix,
    val imageType: String, // background/mask/foreground
)

class CollageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseCoroutineView(context, attrs, defStyleAttr) {

    private val clipRect = RectF()
    private var pictureSolution: Solution? = null
    private val pictureList = mutableListOf<PictureInfo>()

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.FILL
            isFilterBitmap = true
        }
    }

    private val borderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.STROKE
            strokeWidth = dp2Px(3)
            color = Color.RED
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pictureSolution?.let {
            setupPictures(it)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(clipRect)
        pictureList.forEach {
            canvas.save()
            canvas.clipPath(it.imagePath)
            it.ninePatchDrawable?.let { drawable ->
//                    paint.color = Color.GREEN
//                    canvas.drawRect(it.imageRect, borderPaint)
                drawable.draw(canvas)
            } ?: run {
                canvas.drawBitmap(it.imageBitmap, it.imageMatrix, paint)
            }
            canvas.restore()
        }
    }

    fun setPictureSolution(solution: Solution) {
        this.pictureSolution = solution
        setupPictures(solution)
    }

    private fun setupPictures(solution: Solution) {
        Log.w("CollageView", "setupPictures: $solution")
        if (width == 0 || height == 0) return
        // 根据solution计算出clipPath，clip区域沿图片尺寸中心摆放
        val solutionWidth = solution.width
        val solutionHeight = solution.height ?: height
        var dw = width.toFloat()
        var dh = dw * solutionHeight / solutionWidth
        if (dh > height) {
            dh = height.toFloat()
            dw = dh * solutionWidth / solutionHeight
        }
        val sx = dw / solutionWidth
        val sy = dh / solutionHeight
        val dx = (width - solutionWidth * sx) / 2f
        val dy = (height - solutionHeight * sy) / 2f
        clipRect.set(dx, dy, dx + dw, dy + dh)
        Log.w("CollageView", "setupPictures, R1: ${solutionWidth.toFloat() / solutionHeight}, w:h: ${solutionWidth}x$solutionHeight, R2: ${dw / dh}, sx: $sx, sy: $sy, dx: $dx, dy: $dy")

        launch {
            pictureList.clear()
            pictureList.addAll(decodePictures(solution, sx, sy, dx, dy))
            invalidate()
        }
    }

    private val colorList = mutableListOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.LTGRAY, Color.DKGRAY)

    private suspend fun decodePictures(solution: Solution, sx: Float, sy: Float, dx: Float, dy: Float): List<PictureInfo> = withContext(Dispatchers.IO) {
        Log.d("CollageView", "decodePictures: scale: $sx, dx: $dx, dy: $dy")
        val pictureList = mutableListOf<PictureInfo>()
        solution.pictures.forEachIndexed { index, picture ->
            val left = dx + picture.left * sx
            val top = dy + picture.top * sy
            val right = left + (picture.right - picture.left) * sx
            val bottom = top + (picture.bottom - picture.top) * sy
            val imageRect = RectF(left, top, right, bottom)
            val imagePath = Path().apply {
                addRect(imageRect, Path.Direction.CW)
            }
            val path = /*picture.hint ?: */picture.src
            val imageBitmap = context.assets.open("collage/$path").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
            // Log.d("CollageView", "decodePictures, id: ${picture.id}, imageRect $imageRect, width: ${imageRect.width()}, height: ${imageRect.height()}, ninePatchChunk: ${imageBitmap.ninePatchChunk}")
            // Log.w("CollageView", "decodePictures, imageRect ratio: ${imageRect.width() / imageRect.height()}, image ratio: ${(picture.right - picture.left).toFloat() / (picture.bottom - picture.top).toFloat()}")
            val ninePatchChunk = imageBitmap.ninePatchChunk
            val ninePatchDrawable = if (ninePatchChunk != null) {
                val isNinePatchChunk = NinePatch.isNinePatchChunk(ninePatchChunk)
                Log.w("CollageView", "decodePictures, id: ${picture.id}, isNinePatchChunk: $isNinePatchChunk")
                val rect = Rect(imageRect.left.roundToInt(), imageRect.top.roundToInt(), imageRect.right.roundToInt(), imageRect.bottom.roundToInt())
                NinePatchDrawable(resources, imageBitmap, ninePatchChunk, rect, null).apply {
                    setTint(colorList[index % colorList.size])
                    bounds = rect
                }
            } else {
                null
            }
            val imageMatrix = Matrix().apply {
                setRectToRect(RectF(0f, 0f, imageBitmap.width.toFloat(), imageBitmap.height.toFloat()), imageRect, Matrix.ScaleToFit.FILL)
            }
            pictureList.add(PictureInfo(imageRect, imagePath, imageBitmap, ninePatchDrawable, imageMatrix, picture.type))
        }
        pictureList
    }
}