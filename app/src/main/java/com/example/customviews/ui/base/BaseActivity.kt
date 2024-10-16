package com.example.customviews.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.example.customviews.R

abstract class BaseActivity<V : ViewBinding>(open val block: (LayoutInflater) -> V) : AppCompatActivity() {

    protected val binding: V by lazy { block(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (enableInsets()) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            }
            insets
        }
        initActivity(savedInstanceState)
    }

    protected open fun enableInsets(): Boolean = true

    abstract fun initActivity(savedInstanceState: Bundle?)
}