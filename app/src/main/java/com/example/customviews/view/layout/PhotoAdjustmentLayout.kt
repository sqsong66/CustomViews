package com.example.customviews.view.layout

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.customviews.R
import com.example.customviews.data.PhotoAdjustmentData
import com.example.customviews.databinding.LayoutPhotoAdjustBinding
import com.example.customviews.ui.adapter.PhotoAdjustmentAdapter
import com.example.customviews.utils.VibratorHelper
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.ext.measureViewSize
import com.example.customviews.utils.getAdjustmentFilterData
import com.example.customviews.utils.getThemeColor
import com.example.customviews.utils.screenWidth
import com.example.customviews.view.RulerView
import com.sqsong.opengllib.filters.BaseImageFilter
import com.sqsong.opengllib.filters.ComposeAdjustImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class PhotoAdjustmentLayout(
    private val activity: AppCompatActivity,
    rootView: ViewGroup,
    imageBitmap: Bitmap,
    private val resultCallback: (Bitmap?) -> Unit,
    removeCallback: () -> Unit
) : BaseCustomLayout<LayoutPhotoAdjustBinding>(LayoutPhotoAdjustBinding::inflate, rootView, removeCallback), View.OnClickListener {

    private var selectPosition = 0
    private var isClickScroll = false
    private val vibratorHelper by lazy { VibratorHelper(activity) }
    private val baseImageFilter by lazy { BaseImageFilter(activity) }
    private val adjustGroupImageFilter by lazy { ComposeAdjustImageFilter(activity) }

    private val filterAdapter by lazy {
        PhotoAdjustmentAdapter(onItemClick = {
            isClickScroll = true
            updateAdapterCheckedIndex(it)
            scrollPosition(it)
        }, onUpdateFilter = {
            val progress = (it.intensity - it.minValue) / (it.maxValue - it.minValue)
            binding.glSurfaceView.setProgress(progress, it.filterMode)
        }) { data, isInit ->
            // Log.d("PhotoFilterLayout", "onFilterChanged: ")
            updateFilterInfo(data, isInit)
        }
    }

    private val recyclerScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isClickScroll = false
                if (selectPosition == 0) {
                    scrollPosition(0)
                } else if (selectPosition == filterAdapter.itemCount - 1) {
                    scrollPosition(filterAdapter.itemCount - 1)
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (isClickScroll) return // 如果是点击滚动到相应位置则不监听它的滚动事件
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
            val centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2
            if (centerPosition != selectPosition) {
                updateAdapterCheckedIndex(centerPosition)
                // Log.d("PhotoFilterLayout", "selectPosition: $selectPosition")
            }
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            binding.glSurfaceView.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            binding.glSurfaceView.onPause()
        }
    }

    init {
        initLayout()
        showLayout()
        initListeners()
        binding.recycler.adapter = filterAdapter
        binding.glSurfaceView.setImageBitmap(imageBitmap)
    }

    @SuppressLint("InflateParams")
    private fun initLayout() {
        LayoutInflater.from(activity).inflate(R.layout.item_photo_adjustment, null).apply {
            measureViewSize()
            // Log.d("PhotoFilterLayout", "initLayout: $measuredHeight, dp2Px(106): ${dp2Px<Int>(106)}")
            binding.recycler.layoutParams.apply {
                height = max(measuredHeight, dp2Px(106))
                binding.recycler.layoutParams = this
                binding.root.requestLayout()
            }
        }

        val surfaceColor = getThemeColor(activity, com.google.android.material.R.attr.colorSurface)
        binding.glSurfaceView.setGlBackgroundColor(surfaceColor)

        val itemSize = (screenWidth - dp2Px<Int>(16) * 4) / 5
        val padding = (screenWidth - itemSize) / 2
        binding.recycler.setPadding(padding, 0, padding, 0)
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.recycler)
        binding.recycler.addOnScrollListener(recyclerScrollListener)

        binding.rulerView.setOnValueChangeListener(object : RulerView.OnValueChangeListener {
            override fun onValueChange(value: Float, isStop: Boolean) {
                val anyNotZero = filterAdapter.updateCheckedItem(value)
                binding.compareTv.visibility = if (anyNotZero) View.VISIBLE else View.GONE
            }
        })
        binding.root.postDelayed({ scrollPosition(0) }, 10)
        binding.glSurfaceView.setFilter(adjustGroupImageFilter)

        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                getAdjustmentFilterData(activity)
            }.let {
                filterAdapter.submitFilterData(it)
                binding.root.requestLayout()
            }
        }
    }

    private fun updateAdapterCheckedIndex(position: Int) {
        selectPosition = position
        filterAdapter.updateCheckedIndex(selectPosition)
    }

    private fun scrollPosition(position: Int) {
        val smoothScroller = object : LinearSmoothScroller(activity) {
            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                val midpoint = (boxStart + boxEnd) / 2  // 计算RecyclerView中心
                val targetMidpoint = (viewStart + viewEnd) / 2  // 计算项的中心
                return midpoint - targetMidpoint  // 计算滚动所需的距离
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 80f / displayMetrics.densityDpi
            }
        }
        smoothScroller.targetPosition = position
        binding.recycler.layoutManager?.startSmoothScroll(smoothScroller)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initListeners() {
        binding.doneIv.setOnClickListener(this)
        binding.cancelIv.setOnClickListener(this)
        binding.compareTv.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    binding.glSurfaceView.setFilter(baseImageFilter)
                }

                MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    binding.glSurfaceView.setFilter(adjustGroupImageFilter)
                }
            }
            true
        }
        activity.lifecycle.addObserver(lifecycleObserver)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cancelIv -> removeLayout(false)
            R.id.doneIv -> {
                binding.glSurfaceView.queueEvent {
                    binding.glSurfaceView.getRenderedBitmap()?.let { bitmap ->
                        binding.root.post {
                            resultCallback(bitmap)
                            removeLayout(false)
                        }
                    }
                }
            }
        }
    }

    private fun updateFilterInfo(adjustmentData: PhotoAdjustmentData, isInit: Boolean) {
        if (!isInit) vibratorHelper.vibrate()
        binding.rulerView.setCurrentValue(adjustmentData.intensity, adjustmentData.minValue, adjustmentData.maxValue, adjustmentData.stepUnit)
    }

    override fun onLayoutRemoved() {
        activity.lifecycle.removeObserver(lifecycleObserver)
        binding.glSurfaceView.onDestroy()
        super.onLayoutRemoved()
    }

}