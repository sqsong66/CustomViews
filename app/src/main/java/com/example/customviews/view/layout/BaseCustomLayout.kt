package com.example.customviews.view.layout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BaseCustomLayout<V : ViewBinding>(
    val block: (LayoutInflater, ViewGroup, Boolean) -> V,
    val viewGroup: ViewGroup,
    private val callback: () -> Unit = {}
) {

    protected val binding: V by lazy { block(LayoutInflater.from(viewGroup.context), viewGroup, true) }

    init {
        // 拦截事件，防止事件穿透到后面
        binding.root.setOnClickListener { }
    }

    protected open fun initListeners() {}

    protected fun showLayout(animate: Boolean = true) {
        if (animate) {
            binding.root.alpha = 0f
            binding.root.animate().alpha(1f).setDuration(300).start()
        } else {
            binding.root.alpha = 1f
        }
    }

    fun removeLayout(animate: Boolean = true) {
        if (animate) {
            binding.root.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        viewGroup.removeView(binding.root)
                        onLayoutRemoved()
                    }
                })
                .start()
        } else {
            viewGroup.removeView(binding.root)
            onLayoutRemoved()
        }
    }

    protected open fun onLayoutRemoved() {
        callback.invoke()
    }

}