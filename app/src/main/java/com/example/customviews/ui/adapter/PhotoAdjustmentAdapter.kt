package com.example.customviews.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.customviews.data.PhotoAdjustmentData
import com.example.customviews.databinding.ItemPhotoAdjustmentBinding
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.ext.setMaterialShapeBackgroundDrawable
import com.example.customviews.utils.screenWidth

class PhotoAdjustmentAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onUpdateFilter: (PhotoAdjustmentData) -> Unit,
    private val onFilterChanged: (PhotoAdjustmentData, Boolean) -> Unit
) : Adapter<PhotoAdjustmentAdapter.PhotoFilterViewHolder>() {

    private var checkedIndex = 0
    private val filterList = mutableListOf<PhotoAdjustmentData>()
    private val viewSize = (screenWidth - dp2Px<Int>(16) * 4) / 5

    @SuppressLint("NotifyDataSetChanged")
    fun submitFilterData(filterData: List<PhotoAdjustmentData>) {
        filterList.clear()
        filterList.addAll(filterData)
        notifyDataSetChanged()
        onFilterChanged(filterList[checkedIndex], true)
    }

    fun updateCheckedIndex(position: Int) {
        if (position < 0 || position >= filterList.size) return
        val preIndex = checkedIndex
        checkedIndex = position
        if (filterList[preIndex].isValueChangeMode) {
            filterList[preIndex].isValueChangeMode = false
            notifyItemChanged(preIndex)
        }
        if (filterList[checkedIndex].isValueChangeMode) {
            filterList[checkedIndex].isValueChangeMode = false
            notifyItemChanged(checkedIndex)
        }
        onFilterChanged(filterList[checkedIndex], false)
    }

    fun updateCheckedItem(intensity: Float): Boolean {
        if (checkedIndex < 0 || checkedIndex >= filterList.size) return filterList.any { it.intensity != 0f }
        filterList[checkedIndex].intensity = intensity
        filterList[checkedIndex].isValueChangeMode = true
        notifyItemChanged(checkedIndex)
        onUpdateFilter(filterList[checkedIndex])

        // 如果有滤镜值不等于0的，则页面显示对比按钮
        return filterList.any { it.intensity != 0f }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoFilterViewHolder {
        return PhotoFilterViewHolder(ItemPhotoAdjustmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = filterList.size

    override fun onBindViewHolder(holder: PhotoFilterViewHolder, position: Int) {
        holder.bindData(filterList[position], position)
    }

    inner class PhotoFilterViewHolder(private val binding: ItemPhotoAdjustmentBinding) : ViewHolder(binding.root) {

        init {
            binding.indicatorView.setMaterialShapeBackgroundDrawable(allCornerSize = dp2Px(2), backgroundColorResId = com.google.android.material.R.attr.colorSurfaceVariant)
        }

        init {
            binding.root.layoutParams.apply {
                width = viewSize
                binding.root.layoutParams = this
            }
            (binding.editMenuView.layoutParams as MarginLayoutParams).apply {
                width = viewSize
                height = viewSize
                binding.editMenuView.layoutParams = this
            }
        }

        fun bindData(photoAdjustData: PhotoAdjustmentData, position: Int) {
            binding.indicatorView.visibility = if (photoAdjustData.intensity == 0f) View.INVISIBLE else View.VISIBLE
            binding.editMenuView.setIconRes(photoAdjustData.icon, photoAdjustData.isValueChangeMode, photoAdjustData.intensity.toInt().toString())
            binding.menuNameTv.text = photoAdjustData.name
            binding.editMenuView.setOnClickListener {
                if (checkedIndex == position) return@setOnClickListener
                if (filterList[checkedIndex].isValueChangeMode) {
                    filterList[checkedIndex].isValueChangeMode = false
                    notifyItemChanged(checkedIndex)
                }
                checkedIndex = position
                onItemClick(position)
            }
        }
    }

}