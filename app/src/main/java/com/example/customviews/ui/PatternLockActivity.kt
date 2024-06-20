package com.example.customviews.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.customviews.R
import com.example.customviews.databinding.ActivityPatternLockBinding
import com.example.customviews.view.PatternLockActionListener

class PatternLockActivity : AppCompatActivity(), PatternLockActionListener {

    private lateinit var binding: ActivityPatternLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPatternLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.patternLockView.setPatternLockActionListener(this)
    }

    override fun onDrawPatternSuccess(value: String) {
        Log.d("PatternLockActivity", "onDrawPatternSuccess: $value")
    }
}