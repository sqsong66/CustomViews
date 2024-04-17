package com.example.customviews.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.customviews.databinding.ActivityColorPanelBinding
import com.example.customviews.view.OnColorChangedListener

class ColorPanelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityColorPanelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityColorPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.colorPickerView.setColor(Color.CYAN)
//        binding.view.setBackgroundColor(Color.CYAN)

        binding.colorPickerView.setOnColorChangedListener(object : OnColorChangedListener {
            override fun onColorChanged(color: Int, touchType: Int) {
                binding.view.setBackgroundColor(color)
            }
        })
    }
}