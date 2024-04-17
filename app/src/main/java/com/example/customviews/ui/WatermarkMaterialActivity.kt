package com.example.customviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customviews.databinding.ActivityWatermarkMaterialBinding

class WatermarkMaterialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatermarkMaterialBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWatermarkMaterialBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

}