package com.example.customviews

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.example.customviews.databinding.ActivityMainBinding
import com.example.customviews.ui.AdjustShadowActivity
import com.example.customviews.ui.BlurImageActivity
import com.example.customviews.ui.CollegeSampleActivity
import com.example.customviews.ui.ColorPanelActivity
import com.example.customviews.ui.DownloadFontSampleActivity
import com.example.customviews.ui.GLTransitionActivity
import com.example.customviews.ui.GifMakerActivity
import com.example.customviews.ui.ImageFilter2Activity
import com.example.customviews.ui.ImageFilterActivity
import com.example.customviews.ui.ImageLightOnActivity
import com.example.customviews.ui.ImageLutActivity
import com.example.customviews.ui.ImageOutlineActivity
import com.example.customviews.ui.LUTFilterActivity
import com.example.customviews.ui.ManualCutoutActivity
import com.example.customviews.ui.MosaicActivity
import com.example.customviews.ui.OpenGLFilterActivity
import com.example.customviews.ui.OpenGLImageVideoFilterActivity
import com.example.customviews.ui.OpenGLProcessorActivity
import com.example.customviews.ui.OpenGLSampleActivity
import com.example.customviews.ui.OpenGLVideoRecordActivity
import com.example.customviews.ui.PatternLockActivity
import com.example.customviews.ui.RulerSampleActivity
import com.example.customviews.ui.ScalableImageViewActivity
import com.example.customviews.ui.ScaleCanvasActivity
import com.example.customviews.ui.SetPinPatternActivity
import com.example.customviews.ui.SwapFaceResultActivity
import com.example.customviews.ui.TurntableActivity
import com.example.customviews.ui.VideoTransformerActivity
import com.example.customviews.ui.WatermarkMaterialActivity

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var requestUriType = 0
    private lateinit var binding: ActivityMainBinding

    private val requestImageLaunch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                val uris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    // uris.add(clipData.getItemAt(i).uri)
                    clipData.getItemAt(i).uri?.let { uri ->
                        uris.add(uri)
                    }
                    Log.d("songmao", "uri: ${clipData.getItemAt(i).uri}")
                }
                launchActivity(uris)
            } ?: result.data?.data?.let {
                launchActivity(listOf(it))
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
        binding.openGlFilterBtn.setOnClickListener(this)
        binding.blurImageBtn.setOnClickListener(this)
        binding.videoTransformerBtn.setOnClickListener(this)
        binding.openglImageVideoFilterBtn.setOnClickListener(this)
        binding.fontSampleBtn.setOnClickListener(this)
        binding.patternLockBtn.setOnClickListener(this)
        binding.glTransitionBtn.setOnClickListener(this)
        binding.gifMakerBtn.setOnClickListener(this)
        binding.adjustShadowBtn.setOnClickListener(this)
        binding.lightonBtn.setOnClickListener(this)
        binding.openglRecordBtn.setOnClickListener(this)
        binding.imageLutBtn.setOnClickListener(this)
        binding.collageBtn.setOnClickListener(this)
        binding.imageOutlineBtn.setOnClickListener(this)
        binding.manualCutoutBtn.setOnClickListener(this)
        binding.setPinPatternBtn.setOnClickListener(this)
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
                startLaunchImageActivity(3)
            }

            R.id.openGlFilterBtn -> {
                startLaunchImageActivity(4)
            }

            R.id.blurImageBtn -> {
                startLaunchImageActivity(5)
            }

            R.id.videoTransformerBtn -> {
                startLaunchImageActivity(6, 1)
            }

            R.id.openglImageVideoFilterBtn -> {
                startLaunchImageActivity(7)
            }

            R.id.fontSampleBtn -> {
                startActivity(Intent(this, DownloadFontSampleActivity::class.java))
            }

            R.id.patternLockBtn -> {
                startActivity(Intent(this, PatternLockActivity::class.java))
            }

            R.id.glTransitionBtn -> {
                startActivity(Intent(this, GLTransitionActivity::class.java))
            }

            R.id.gifMakerBtn -> {
                // startActivity(Intent(this, GifMakerActivity::class.java))
                startLaunchImageActivity(8, isMulti = true)
            }

            R.id.adjustShadowBtn -> {
                startActivity(Intent(this, AdjustShadowActivity::class.java))
            }

            R.id.lightonBtn -> {
                startLaunchImageActivity(9)
            }

            R.id.openglRecordBtn -> {
                startLaunchImageActivity(10, isMulti = true)
                // startActivity(Intent(this, OpenGLVideoRecordActivity::class.java))
            }

            R.id.imageLutBtn -> {
                startLaunchImageActivity(11)
            }

            R.id.collageBtn -> {
                // startActivity(Intent(this, CollegeSampleActivity::class.java))
                startLaunchImageActivity(12, isMulti = true)
            }

            R.id.imageOutlineBtn -> {
                startLaunchImageActivity(13)
            }

            R.id.manualCutoutBtn -> {
                startActivity(Intent(this, ManualCutoutActivity::class.java))
            }

            R.id.setPinPatternBtn -> {
                startActivity(Intent(this, SetPinPatternActivity::class.java))
            }
        }
    }

    private fun startLaunchImageActivity(requestType: Int, mediaType: Int = 0, isMulti: Boolean = false) {
        requestUriType = requestType
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = if (mediaType == 0) "image/*" else "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMulti)
        }
        requestImageLaunch.launch(intent)
    }

    @OptIn(UnstableApi::class)
    private fun launchActivity(uris: List<Uri>) {
        when (requestUriType) {
            0 -> {
                startActivity(Intent(this, MosaicActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            1 -> {
                startActivity(Intent(this, LUTFilterActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            2 -> {
                startActivity(Intent(this, OpenGLSampleActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            3 -> {
                startActivity(Intent(this, RulerSampleActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            4 -> {
                startActivity(Intent(this, OpenGLFilterActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            5 -> {
                startActivity(Intent(this, BlurImageActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            6 -> {
                startActivity(Intent(this, VideoTransformerActivity::class.java).apply {
                    putExtra("videoUri", uris.firstOrNull())
                })
            }

            7 -> {
                startActivity(Intent(this, OpenGLImageVideoFilterActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            8 -> {
                startActivity(Intent(this, GifMakerActivity::class.java).apply {
                    putParcelableArrayListExtra("imageUris", ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }

            9 -> {
                startActivity(Intent(this, ImageLightOnActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            10 -> {
                startActivity(Intent(this, OpenGLVideoRecordActivity::class.java).apply {
                    putParcelableArrayListExtra("imageUris", ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }

            11 -> {
                startActivity(Intent(this, ImageLutActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }

            12 -> {
                startActivity(Intent(this, CollegeSampleActivity::class.java).apply {
                    putParcelableArrayListExtra("imageUris", ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }

            13 -> {
                startActivity(Intent(this, ImageOutlineActivity::class.java).apply {
                    putExtra("imageUri", uris.firstOrNull())
                })
            }
        }
    }

}