package com.example.customviews.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.data.Solution
import com.example.customviews.databinding.ActivityCollegeSampleBinding
import com.example.customviews.ui.adapter.CollagePreviewAdapter
import com.example.customviews.ui.base.BaseActivity
import com.example.customviews.utils.parseCollages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CollegeSampleActivity : BaseActivity<ActivityCollegeSampleBinding>(ActivityCollegeSampleBinding::inflate) {

    private var imageUris: List<Uri>? = null
    private var collageList: List<Solution>? = null
    private val collagePreviewAdapter by lazy {
        CollagePreviewAdapter {
            binding.collageView.setPictureSolution(it)
        }
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        imageUris = IntentCompat.getParcelableArrayListExtra(intent, "imageUris", Uri::class.java) ?: throw IllegalArgumentException("Image uris are required.")
        loadCollageConfig(imageUris?.size ?: 2)
        initLayout()
    }

    private fun initLayout() {
        binding.recyclerView.adapter = collagePreviewAdapter
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.templateBtn -> loadCollageConfig(imageUris?.size ?: 2)
                    R.id.posterBtn -> loadCollageConfig(imageUris?.size ?: 2, "poster")
                    R.id.joinBtn -> loadCollageConfig(imageUris?.size ?: 2, "join")
                }
            }
        }
    }

    private fun loadCollageConfig(imageSize: Int, type: String = "template") {
        flow {
            val solutionList = collageList ?: parseCollages(this@CollegeSampleActivity).apply { collageList = this }
            emit(solutionList.filter { it.type == type && (it.pictures.count { picture -> picture.isMask() } == imageSize || it.pictureAreas.isNotEmpty()) })
        }.flowOn(Dispatchers.IO)
            .onEach { collageList ->
                collageList.forEach {
                    Log.w("XmlUtils", "loadCollageConfig: $it")
                }
                collagePreviewAdapter.submitList(collageList)
            }
            .launchIn(lifecycleScope)
    }

}