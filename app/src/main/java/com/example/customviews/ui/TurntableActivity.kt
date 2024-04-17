package com.example.customviews.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customviews.databinding.ActivityTurntableBinding
import com.example.customviews.view.OnTurntableListener
import com.example.customviews.view.TurntableInfo

class TurntableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTurntableBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTurntableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startBtn.setOnClickListener {
            binding.currentValueTv.text = "???"
            binding.turntableView.resetTurntable()
        }

        binding.turntableView.setOnTurntableListener(object : OnTurntableListener {
            override fun onRotate(info: TurntableInfo) {
                binding.currentValueTv.text = info.title
            }

            override fun onRotateEnd(info: TurntableInfo) {
                binding.currentValueTv.text = info.title
            }
        })
    }
}