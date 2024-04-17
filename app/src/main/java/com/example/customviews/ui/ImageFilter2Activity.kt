package com.example.customviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customviews.databinding.ActivityImageFilter2Binding

class ImageFilter2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityImageFilter2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageFilter2Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}