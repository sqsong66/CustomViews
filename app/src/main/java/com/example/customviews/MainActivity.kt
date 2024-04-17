package com.example.customviews

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.customviews.databinding.ActivityMainBinding
import com.example.customviews.ui.ColorPanelActivity
import com.example.customviews.ui.ImageFilter2Activity
import com.example.customviews.ui.ImageFilterActivity
import com.example.customviews.ui.LUTFilterActivity
import com.example.customviews.ui.MosaicActivity
import com.example.customviews.ui.OpenGLProcessorActivity
import com.example.customviews.ui.OpenGLSampleActivity
import com.example.customviews.ui.RulerSampleActivity
import com.example.customviews.ui.ScalableImageViewActivity
import com.example.customviews.ui.ScaleCanvasActivity
import com.example.customviews.ui.SwapFaceResultActivity
import com.example.customviews.ui.TurntableActivity
import com.example.customviews.ui.WatermarkMaterialActivity

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var requestUriType = 0
    private lateinit var binding: ActivityMainBinding

    private val requestImageLaunch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let {
                launchActivity(it)
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.watermarkMaterialView.setOnClickListener(this)
        binding.imageFilterView.setOnClickListener(this)
        binding.turntableView.setOnClickListener(this)
        binding.scaleCanvasView.setOnClickListener(this)
        binding.colorPanelView.setOnClickListener(this)
        binding.scalableImageView.setOnClickListener(this)
        binding.imageFilter2View.setOnClickListener(this)
        binding.openGlProcessBtn.setOnClickListener(this)
        binding.mosaicViewBtn.setOnClickListener(this)
        binding.lutFilterBtn.setOnClickListener(this)
        binding.swapFaceBtn.setOnClickListener(this)
        binding.openGlSampleBtn.setOnClickListener(this)
        binding.rulerBtn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.watermarkMaterialView -> {
                startActivity(Intent(this, WatermarkMaterialActivity::class.java))
            }

            R.id.imageFilterView -> {
                startActivity(Intent(this, ImageFilterActivity::class.java))
            }

            R.id.turntableView -> {
                startActivity(Intent(this, TurntableActivity::class.java))
            }

            R.id.scaleCanvasView -> {
                startActivity(Intent(this, ScaleCanvasActivity::class.java))
            }

            R.id.colorPanelView -> {
                startActivity(Intent(this, ColorPanelActivity::class.java))
            }

            R.id.scalableImageView -> {
                startActivity(Intent(this, ScalableImageViewActivity::class.java))
            }

            R.id.imageFilter2View -> {
                startActivity(Intent(this, ImageFilter2Activity::class.java))
            }

            R.id.openGlProcessBtn -> {
                startActivity(Intent(this, OpenGLProcessorActivity::class.java))
            }

            R.id.mosaicViewBtn -> {
                startLaunchImageActivity(0)
            }

            R.id.lutFilterBtn -> {
                startLaunchImageActivity(1)
            }

            R.id.swapFaceBtn -> {
                startActivity(Intent(this, SwapFaceResultActivity::class.java))

            }

            R.id.openGlSampleBtn -> {
                startLaunchImageActivity(2)
            }

            R.id.rulerBtn -> {
                startActivity(Intent(this, RulerSampleActivity::class.java))
            }
        }
    }

    private fun startLaunchImageActivity(requestType: Int) {
        requestUriType = requestType
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        requestImageLaunch.launch(intent)
    }

    private fun launchActivity(uri: Uri) {
        when (requestUriType) {
            0 -> {
                startActivity(Intent(this, MosaicActivity::class.java).apply {
                    putExtra("imageUri", uri)
                })
            }

            1 -> {
                startActivity(Intent(this, LUTFilterActivity::class.java).apply {
                    putExtra("imageUri", uri)
                })
            }

            2 -> {
                startActivity(Intent(this, OpenGLSampleActivity::class.java).apply {
                    putExtra("imageUri", uri)
                })
            }
        }
    }

}