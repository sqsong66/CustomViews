package com.example.customviews.ui.adapter

import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.customviews.R
import com.example.customviews.data.FontsData
import com.example.customviews.databinding.ItemFontSampleBinding

class FontsItemAdapter(
    private val itemClick: (Typeface?) -> Unit
) : RecyclerView.Adapter<FontsItemAdapter.FontsItemViewHolder>() {

    private val fontList = mutableListOf<FontsData>()
    private val fontsCache = SparseArray<Typeface>()
    private val fontsDownloadHandler by lazy {
        val handlerThread = HandlerThread("fonts")
        handlerThread.start()
        Handler(handlerThread.looper)
    }

    fun submitList(list: List<FontsData>) {
        fontList.clear()
        fontList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontsItemViewHolder {
        return FontsItemViewHolder(
            ItemFontSampleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = fontList.size

    override fun onBindViewHolder(holder: FontsItemViewHolder, position: Int) {
        holder.bindData(fontList[position], position)
    }

    inner class FontsItemViewHolder(private val binding: ItemFontSampleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(fontsData: FontsData, position: Int) {
            binding.fontNameTv.text = fontsData.fontName
            binding.fontPreviewTv.text = fontsData.previewText

            val cachedTypeface = fontsCache[position]
            if (cachedTypeface != null) {
                binding.fontPreviewTv.typeface = cachedTypeface
                binding.fontPreviewTv.visibility = View.VISIBLE
                binding.loadingProgress.visibility = View.GONE
            } else {
                binding.fontPreviewTv.visibility = View.INVISIBLE
                binding.fontPreviewTv.typeface = Typeface.DEFAULT
                binding.loadingProgress.visibility = View.VISIBLE

                val request = FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    fontsData.fontName,
                    R.array.com_google_android_gms_fonts_certs
                )

                FontsContractCompat.requestFont(
                    binding.root.context, request,
                    object : FontsContractCompat.FontRequestCallback() {
                        override fun onTypefaceRetrieved(typeface: Typeface?) {
                            super.onTypefaceRetrieved(typeface)
                            fontsCache.put(position, typeface)
                            fontsData.typeface = typeface
                            binding.fontPreviewTv.typeface = typeface
                            binding.fontPreviewTv.visibility = View.VISIBLE
                            binding.loadingProgress.visibility = View.GONE
                        }

                        override fun onTypefaceRequestFailed(reason: Int) {
                            super.onTypefaceRequestFailed(reason)
                            Log.e("FontsItemAdapter", "onTypefaceRequestFailed: $reason，fontName: ${fontsData.fontName}")
                        }
                    },
                    fontsDownloadHandler
                )
            }

            binding.root.setOnClickListener {
                itemClick(fontsData.typeface)
            }
        }
    }

}
