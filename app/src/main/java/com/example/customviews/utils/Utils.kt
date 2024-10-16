package com.example.customviews.utils

import android.content.Context
import com.example.customviews.R
import com.example.customviews.data.ImageFilterData
import com.example.customviews.data.PhotoAdjustmentData
import com.sqsong.opengllib.filters.FilterMode

fun range(value: Float, start: Float, end: Float): Float {
    return (end - start) * value + start
}

fun getAdjustmentFilterData(context: Context): List<PhotoAdjustmentData> {
    val dataList = mutableListOf<PhotoAdjustmentData>()
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_BRIGHTNESS, context.getString(R.string.key_adjust_brightness), R.drawable.ic_adjustment_brightness, 0f, false, -100f, 100f, 4f, true))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_TEMPERATURE, context.getString(R.string.key_adjust_temperature), R.drawable.ic_adjustment_temperature, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_CONTRAST, context.getString(R.string.key_adjust_contrast), R.drawable.ic_adjustment_contrast, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SATURATION, context.getString(R.string.key_adjust_saturation), R.drawable.ic_adjustment_saturation, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_HIGHLIGHT, context.getString(R.string.key_adjust_highlight), R.drawable.ic_adjustment_highlights, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SHADOW, context.getString(R.string.key_adjust_shadow), R.drawable.ic_adjustment_shadow, 0f, false, -100f, 100f, 4f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_SHARPNESS, context.getString(R.string.key_adjust_sharpness), R.drawable.ic_adjustment_sharpen, 0f, false, 0f, 100f, 2f))
    dataList.add(PhotoAdjustmentData(FilterMode.FILTER_TINT, "Tint", R.drawable.ic_adjustment_temperature, 0f, false, -100f, 100f, 4f))
    return dataList
}

fun getLutFilterData(): List<ImageFilterData> {
    val dataList = mutableListOf<ImageFilterData>()
    dataList.add(ImageFilterData("Original", null, "", 1.0f, null, true))
    dataList.add(ImageFilterData("Avery", listOf("lut/toaster_metal.png", "lut/toaster_soft_light.png", "lut/toaster_curves.png", "lut/toaster_overlay_map_warm.png", "lut/toaster_color_shift.png"), "shader/filter_lut_avery.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Ela", listOf("lut/walden_map.png", "lut/vignette_map.png"), "shader/filter_lut_ela.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Emma", listOf("lut/nmap.png", "lut/nblowout.png"), "shader/filter_lut_emma.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Ava", listOf("lut/brannan_process.png", "lut/brannan_blowout.png", "lut/brannan_contrast.png", "lut/brannan_luma.png", "lut/brannan_screen.png"), "shader/filter_lut_ava.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Harper", listOf("lut/blackboard.png", "lut/overlay_map.png", "lut/rise_map.png"), "shader/filter_lut_harper.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Elizabeth", listOf("lut/vignette_map.png", "lut/sutro_metal.png", "lut/soft_light.png", "lut/sutro_edge_burn.png", "lut/sutro_curves.png"), "shader/filter_lut_elizabeth.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Emily", listOf("lut/sierra_vignette.png", "lut/overlay_map.png", "lut/sierra_map.png"), "shader/filter_lut_emily.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Mia", listOf("lut/hudson_background.png", "lut/overlay_map.png", "lut/hudson_map.png"), "shader/filter_lut_mia.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Sophia", listOf("lut/edge_burn.png", "lut/hefe_map.png", "lut/hefe_gradient_map.png", "lut/hefe_soft_light.png", "lut/hefe_metal.png"), "shader/filter_lut_sophia.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Abigail", listOf("lut/nashville_map.png"), "shader/filter_lut_abigail.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Addison", listOf("lut/valencia_map.png", "lut/valencia_gradient_map.png"), "shader/filter_lut_addison.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Charlotte", listOf("lut/inkwell_map.png"), "shader/filter_lut_charlotte.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Issabella", listOf("lut/earlybird_curves.png", "lut/earlybird_overlay_map.png", "lut/vignette_map.png", "lut/earlybird_blowout.png", "lut/earlybird_map.png"), "shader/filter_lut_issabella.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Madison", listOf("lut/xpro_map.png", "lut/vignette_map.png"), "shader/filter_lut_madison.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Olivia", listOf("lut/blackboard.png", "lut/overlay_map.png", "lut/amaro_map.png"), "shader/filter_lut_olivia.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Amelia", listOf("lut/lomo_map.png", "lut/vignette_map.png"), "shader/filter_lut_amelia.glsl", 1.0f, null))
    dataList.add(ImageFilterData("Evelyn", listOf("lut/kelvin_map.png"), "shader/filter_lut_evelyn.glsl", 1.0f, null))

    dataList.add(ImageFilterData("A01", listOf("filter/filter_lut_a01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A02", listOf("filter/filter_lut_a02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A03", listOf("filter/filter_lut_a03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A04", listOf("filter/filter_lut_a04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A05", listOf("filter/filter_lut_a05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A06", listOf("filter/filter_lut_a06.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A07", listOf("filter/filter_lut_a07.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A08", listOf("filter/filter_lut_a08.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A09", listOf("filter/filter_lut_a09.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A10", listOf("filter/filter_lut_a10.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A11", listOf("filter/filter_lut_a11.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A12", listOf("filter/filter_lut_a12.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A13", listOf("filter/filter_lut_a13.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A14", listOf("filter/filter_lut_a14.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("A15", listOf("filter/filter_lut_a15.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B01", listOf("filter/filter_lut_b01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B02", listOf("filter/filter_lut_b02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B03", listOf("filter/filter_lut_b03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B04", listOf("filter/filter_lut_b04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B05", listOf("filter/filter_lut_b05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B06", listOf("filter/filter_lut_b06.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B07", listOf("filter/filter_lut_b07.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("B08", listOf("filter/filter_lut_b08.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // C
    dataList.add(ImageFilterData("C01", listOf("filter/filter_lut_c01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("C02", listOf("filter/filter_lut_c02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("C03", listOf("filter/filter_lut_c03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // D
    dataList.add(ImageFilterData("D01", listOf("filter/filter_lut_d01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("D02", listOf("filter/filter_lut_d02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("D03", listOf("filter/filter_lut_d03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("D04", listOf("filter/filter_lut_d04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // E
    dataList.add(ImageFilterData("E01", listOf("filter/filter_lut_e01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("E02", listOf("filter/filter_lut_e02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("E03", listOf("filter/filter_lut_e03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // F
    dataList.add(ImageFilterData("F01", listOf("filter/filter_lut_f01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F02", listOf("filter/filter_lut_f02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F03", listOf("filter/filter_lut_f03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F04", listOf("filter/filter_lut_f04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F05", listOf("filter/filter_lut_f05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F06", listOf("filter/filter_lut_f06.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F07", listOf("filter/filter_lut_f07.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("F08", listOf("filter/filter_lut_f08.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // G
    dataList.add(ImageFilterData("G01", listOf("filter/filter_lut_g01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G02", listOf("filter/filter_lut_g02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G03", listOf("filter/filter_lut_g03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G04", listOf("filter/filter_lut_g04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G05", listOf("filter/filter_lut_g05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G06", listOf("filter/filter_lut_g06.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("G07", listOf("filter/filter_lut_g07.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // H
    dataList.add(ImageFilterData("H01", listOf("filter/filter_lut_h01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("H02", listOf("filter/filter_lut_h02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("H03", listOf("filter/filter_lut_h03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("H04", listOf("filter/filter_lut_h04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("H05", listOf("filter/filter_lut_h05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // I
    dataList.add(ImageFilterData("I01", listOf("filter/filter_lut_i01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("I02", listOf("filter/filter_lut_i02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("I03", listOf("filter/filter_lut_i03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("I04", listOf("filter/filter_lut_i04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("I05", listOf("filter/filter_lut_i05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    // J
    dataList.add(ImageFilterData("J01", listOf("filter/filter_lut_j01.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("J02", listOf("filter/filter_lut_j02.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("J03", listOf("filter/filter_lut_j03.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("J04", listOf("filter/filter_lut_j04.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    dataList.add(ImageFilterData("J05", listOf("filter/filter_lut_j05.png"), "shader/lut_filter_frag.frag", 1.0f, null))
    return dataList
}