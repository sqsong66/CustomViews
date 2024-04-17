package com.example.customviews.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.example.customviews.databinding.ItemLutBinding

class LutItemAdapter(
    private val onLutItemClickListener: (String, Int) -> Unit
) : Adapter<LutItemAdapter.LutItemViewHolder>() {

    private val lutList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LutItemViewHolder {
        return LutItemViewHolder(ItemLutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int = lutList.size

    override fun onBindViewHolder(holder: LutItemViewHolder, position: Int) {
        holder.bindData(lutList[position], position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(lists: List<String>) {
        lutList.clear()
        lutList.addAll(lists)
        notifyDataSetChanged()
    }

    inner class LutItemViewHolder(private val binding: ItemLutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(assetPath: String, position: Int) {

            Glide.with(binding.lutImage)
                .load("file:///android_asset/lut/$assetPath")
                .into(binding.lutImage)

            binding.root.setOnClickListener {
                onLutItemClickListener(assetPath, position)
            }
        }

    }


}