package com.example.customviews.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.customviews.R
import com.example.customviews.databinding.ActivityScableImageViewBinding

class ScalableImageViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScableImageViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScableImageViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}