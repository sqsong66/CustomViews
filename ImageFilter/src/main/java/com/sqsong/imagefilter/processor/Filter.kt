package com.sqsong.imagefilter.processor

import android.graphics.Bitmap

class Filter(filter: Filter? = null) {

    private val subFilters = mutableListOf<SubFilter>()

    init {
        filter?.subFilters?.let {
            subFilters.addAll(it)
        }
    }

    fun addSubFilter(subFilter: SubFilter) {
        subFilters.add(subFilter)
    }

    fun addSubFilters(subFilters: List<SubFilter>) {
        this.subFilters.addAll(subFilters)
    }

    fun getSubFilters(): List<SubFilter> {
        return subFilters
    }

    fun clearSubFilters() {
        subFilters.clear()
    }

    fun getSubFilterByTag(tag: Any): SubFilter? {
        return subFilters.find { it.tag == tag }
    }

    fun removeSubFilterByTag(tag: Any) {
        subFilters.removeAll { it.tag == tag }
    }

    fun process(input: Bitmap): Bitmap {
        var output = input
        subFilters.forEach {
            output = try {
                it.process(output)
            } catch (oe: OutOfMemoryError) {
                oe.printStackTrace()
                System.gc()
                it.process(output)
            }
        }
        return output
    }

}