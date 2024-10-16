package com.example.customviews.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup.MarginLayoutParams
import com.example.customviews.common.adapter.AbstractItemAdapter
import com.example.customviews.data.ImageFilterData
import com.example.customviews.databinding.ItemLutPreviewBinding
import com.example.customviews.utils.dp2Px

class LutPreviewAdapter(
    private val onFilterChanged: (ImageFilterData) -> Unit
) : AbstractItemAdapter<ImageFilterData, ItemLutPreviewBinding>(ItemLutPreviewBinding::inflate) {

    private var checkIndex = 0

    @SuppressLint("NotifyDataSetChanged")
    fun addNewData(data: ImageFilterData, clear: Boolean) {
        if (clear) dataList.clear()
        dataList.add(data)
        notifyDataSetChanged()
        if (clear) onFilterChanged(data)
    }

    override fun inflateData(binding: ItemLutPreviewBinding, data: ImageFilterData, position: Int) {
        (binding.root.layoutParams as MarginLayoutParams).apply {
            marginStart = if (position == 0) dp2Px<Int>(16) else dp2Px<Int>(4)
            marginEnd = if (position == dataList.size - 1) dp2Px<Int>(16) else dp2Px<Int>(4)
            binding.root.layoutParams = this
        }

        binding.templateView.setImageBitmap(data.previewBitmap)
        binding.templateView.setChecked(position == checkIndex)
        binding.nameTv.text = data.filterName

        binding.root.setOnClickListener {
            if (checkIndex == position) return@setOnClickListener
            val preIndex = checkIndex
            checkIndex = position
            notifyItemChanged(preIndex)
            notifyItemChanged(checkIndex)
            onFilterChanged(data)
        }
    }

    fun updateCurrentFilterIntensity(value: Float) {
        val data = dataList[checkIndex]
        data.intensity = value
    }

}