package com.example.customviews.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.customviews.databinding.ItemRulerSampleBinding
import com.example.customviews.utils.dp2Px
import com.example.customviews.utils.screenWidth

class RulerSampleAdapter(
    private val onItemClick: (Int) -> Unit
) : Adapter<RulerSampleAdapter.RulerSampleViewHolder>() {

    private val itemSize = (screenWidth - dp2Px<Int>(16) * 4) / 5

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RulerSampleViewHolder {
        return RulerSampleViewHolder(ItemRulerSampleBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = 30

    override fun onBindViewHolder(holder: RulerSampleViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class RulerSampleViewHolder(private val binding: ItemRulerSampleBinding) : ViewHolder(binding.root) {

        fun bind(position: Int) {
            (binding.root.layoutParams as MarginLayoutParams).apply {
                width = itemSize
                height = itemSize
                binding.root.layoutParams = this
            }

            binding.root.setOnClickListener {
                onItemClick(position)
            }
        }
    }

}