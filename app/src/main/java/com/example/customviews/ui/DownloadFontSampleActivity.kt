package com.example.customviews.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.customviews.R
import com.example.customviews.data.FontsData
import com.example.customviews.databinding.ActivityDownloadFontSampleBinding
import com.example.customviews.ui.adapter.FontsItemAdapter

class DownloadFontSampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadFontSampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDownloadFontSampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initLayout()
    }

    private fun initLayout() {
        val fontsAdapter = FontsItemAdapter {
            binding.previewText.typeface = it
        }
        binding.fontRecycler.adapter = fontsAdapter
        resources.getStringArray(R.array.family_names).map { FontsData(fontName = it) }.let {
            fontsAdapter.submitList(it)
        }
    }
}