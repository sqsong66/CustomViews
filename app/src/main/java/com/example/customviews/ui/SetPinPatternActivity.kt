package com.example.customviews.ui

import android.os.Bundle
import com.example.customviews.databinding.ActivitySetPinPatternBinding
import com.example.customviews.ui.base.BaseActivity

class SetPinPatternActivity : BaseActivity<ActivitySetPinPatternBinding>(ActivitySetPinPatternBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {
        initLayout()
        initListeners()
    }

    private fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initLayout() {

    }

}