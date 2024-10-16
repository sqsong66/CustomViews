package com.example.customviews.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.example.customviews.R
import com.example.customviews.databinding.ActivityManualCutoutBinding
import com.example.customviews.ui.base.BaseActivity
import com.example.customviews.utils.decodeBitmapFromAssets
import com.example.customviews.utils.ext.letIfNotNull
import com.example.customviews.view.BrushMode
import com.example.customviews.view.ManualCutoutActionListener
import com.example.customviews.view.SmearMode
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import com.wangxutech.picwish.libnative.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ManualCutoutActivity : BaseActivity<ActivityManualCutoutBinding>(ActivityManualCutoutBinding::inflate), View.OnClickListener, ManualCutoutActionListener {

    override fun enableInsets(): Boolean {
        return false
    }

    override fun initActivity(savedInstanceState: Bundle?) {
        initListeners()
        loadBitmapsFromAssets()
    }

    private fun initListeners() {
        binding.previewImage.setOnClickListener(this)
        binding.resetRb.setOnClickListener(this)
        binding.polygonUndoTv.setOnClickListener(this)
        binding.polygonApplyTv.setOnClickListener(this)
        binding.undoIv.setOnClickListener(this)
        binding.redoIv.setOnClickListener(this)
        binding.doneBtn.setOnClickListener(this)
        binding.manualCutoutView.setManualCutoutActionListener(this)
        onUndoRedoStateChanged(isUndoEnable = false, isRedoEnable = false)
        binding.operationRg.setOnCheckedChangeListener { _, checkedId ->
            binding.manualCutoutView.setBrushMode(if (checkedId == R.id.eraserRb) BrushMode.ERASE else BrushMode.RESERVE)
        }
        binding.smearingMethodRg.setOnCheckedChangeListener { _, checkedId ->
            binding.polygonApplyLayout.visibility = if (checkedId == R.id.polygonRb) View.VISIBLE else View.GONE
            binding.brushSizeSlider.visibility = if (checkedId == R.id.brushRb) View.VISIBLE else View.GONE
            val mode = when (checkedId) {
                R.id.lassoRb -> SmearMode.LASSO
                R.id.polygonRb -> SmearMode.POLYGON
                R.id.rectangleRb -> SmearMode.RECTANGLE
                else -> SmearMode.BRUSH
            }
            binding.manualCutoutView.setSmearMode(mode)
        }
        binding.brushSizeSlider.addOnChangeListener { _, value, _ ->
            binding.manualCutoutView.setBrushSize(value, true)
        }
        binding.brushSizeSlider.addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                binding.manualCutoutView.setBrushSize(slider.value, true)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.manualCutoutView.setBrushSize(slider.value, false)
            }
        })
    }

    private fun loadBitmapsFromAssets() {
        flow {
            val srcBitmap = decodeBitmapFromAssets(applicationContext, "image/origin.jpg", 2048) ?: throw IllegalStateException("srcBitmap is null")
            val maskBitmap = decodeBitmapFromAssets(applicationContext, "image/mask.jpg", 2048)?.let {
                NativeLib.convertMaskToAlphaChannel(it).extractAlpha()
            }
            val cutoutBitmap = decodeBitmapFromAssets(applicationContext, "image/cutout.png", 2048)?.let { cutout ->
                val bitmap = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawBitmap(cutout, 0f, 32f, null)
                bitmap
            }
            /*val srcBitmap = decodeBitmapFromAssets(applicationContext, "image/origin1.jpg", 2048) ?: throw IllegalStateException("srcBitmap is null")
            val maskBitmap = decodeBitmapFromAssets(applicationContext, "image/mask1.jpg", 2048)?.let {
                NativeLib.convertMaskToAlphaChannel(it).extractAlpha()
            }
            val cutoutBitmap = decodeBitmapFromAssets(applicationContext, "image/cutout1.png", 2048)?.let { cutout ->
                val bitmap = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawBitmap(cutout, 0f, 136f, null)
                bitmap
            }*/

            letIfNotNull(srcBitmap, maskBitmap, cutoutBitmap) { src, mask, cutout ->
                emit(Triple(src, mask, cutout))
            }
        }.flowOn(Dispatchers.IO)
            .onEach { (src, mask, cutout) ->
                binding.manualCutoutView.setImageBitmap(src)
                binding.manualCutoutView.setCutoutAndMaskBitmap(cutout, mask)
            }
            .launchIn(lifecycleScope)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.undoIv -> {
                binding.manualCutoutView.undo()
            }

            R.id.redoIv -> {
                binding.manualCutoutView.redo()
            }

            R.id.doneBtn -> {
                flow {
                    binding.manualCutoutView.getResultCutoutBitmap()?.let { emit(it) }
                }.flowOn(Dispatchers.IO)
                    .onEach { bitmap ->
                        binding.previewImage.setImageBitmap(bitmap)
                        binding.previewImage.visibility = View.VISIBLE
                    }
                    .launchIn(lifecycleScope)
            }

            R.id.polygonUndoTv -> {
                binding.manualCutoutView.onPolygonUndo()
            }

            R.id.polygonApplyTv -> {
                binding.manualCutoutView.onPolygonApply()
            }

            R.id.resetRb -> {
                binding.manualCutoutView.resetPaint()
            }

            R.id.previewImage -> {
                binding.previewImage.visibility = View.GONE
            }
        }
    }

    override fun onUndoRedoStateChanged(isUndoEnable: Boolean, isRedoEnable: Boolean) {
        binding.undoIv.isEnabled = isUndoEnable
        binding.redoIv.isEnabled = isRedoEnable
        binding.resetRb.isEnabled = binding.manualCutoutView.isDrawPath
        binding.resetRb.isChecked = false
    }

    override fun onPolygonStateChanged(isPolygonUndoEnable: Boolean, isPolyApplyEnable: Boolean) {
        binding.polygonUndoTv.isEnabled = isPolygonUndoEnable
        binding.polygonApplyTv.isEnabled = isPolyApplyEnable
    }

}