package com.example.customviews.ui

import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.example.customviews.databinding.ActivityRulerSampleBinding
import com.example.customviews.ui.adapter.RulerSampleAdapter
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.screenWidth
import com.example.customviews.view.RulerView

class RulerSampleActivity : AppCompatActivity() {

    private var centerPosition = 0
    private lateinit var binding: ActivityRulerSampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulerSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLayout()
    }

    private fun initLayout() {
        val itemSize = (screenWidth - dp2Px<Int>(16) * 4) / 5
        val padding = (screenWidth - itemSize) / 2
        val layoutManager = binding.rulerSampleRv.layoutManager as LinearLayoutManager

        binding.rulerSampleRv.adapter = RulerSampleAdapter { clickPosition ->
            scrollPosition(clickPosition)
        }

         binding.rulerSampleRv.setPadding(padding, 0, padding, 0)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.rulerSampleRv)

        binding.rulerSampleRv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2
            }
        })
        scrollPosition(0)

        binding.rulerView.setOnValueChangeListener(object : RulerView.OnValueChangeListener {
            override fun onValueChange(value: Float) {
                binding.rulerMaskView.setCurrentValue(value.toInt())
            }
        })
    }

    private fun scrollPosition(position: Int) {
        val smoothScroller = object : LinearSmoothScroller(this) {
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
        binding.rulerSampleRv.layoutManager?.startSmoothScroll(smoothScroller)
    }

}