package com.example.customviews.ui.adapter

import android.view.ViewGroup.MarginLayoutParams
import com.bumptech.glide.Glide
import com.example.customviews.common.adapter.AbstractItemAdapter
import com.example.customviews.data.Solution
import com.example.customviews.databinding.ItemCollagePreviewBinding
import com.example.customviews.utils.dp2Px

class CollagePreviewAdapter(
    private val onCollageSelected: (Solution) -> Unit
) : AbstractItemAdapter<Solution, ItemCollagePreviewBinding>(ItemCollagePreviewBinding::inflate) {

    private var checkedIndex = 0
    private val itemWidth = dp2Px<Int>(66)
    private val itemHeight = itemWidth * 210 / 156

    override fun submitList(list: List<Solution>) {
        checkedIndex = 0
        super.submitList(list)
        onCollageSelected(list[0])
    }

    override fun inflateData(binding: ItemCollagePreviewBinding, data: Solution, position: Int) {
        (binding.root.layoutParams as MarginLayoutParams).apply {
            width = itemWidth
            height = itemHeight
            marginStart = if (position == 0) dp2Px(16) else dp2Px(2)
            marginEnd = if (position == itemCount - 1) dp2Px(16) else dp2Px(2)
            binding.root.layoutParams = this
        }
        Glide.with(binding.aiTemplateView)
            .load("file:///android_asset/collage/${data.thumb}")
            .into(binding.aiTemplateView)
        binding.aiTemplateView.setChecked(checkedIndex == position)

        binding.aiTemplateView.setOnClickListener {
            if (checkedIndex == position) return@setOnClickListener
            val preIndex = checkedIndex
            checkedIndex = position
            notifyItemChanged(preIndex)
            notifyItemChanged(checkedIndex)
            onCollageSelected(data)
        }
    }

}