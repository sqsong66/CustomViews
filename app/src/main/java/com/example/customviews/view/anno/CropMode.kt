package com.sqsong.photoeditor.view.anno

import androidx.annotation.IntDef

@IntDef(
    CropMode.FREE, CropMode.ORIGIN, CropMode.RATIO_1_1, CropMode.RATIO_2_3, CropMode.RATIO_3_2,
    CropMode.RATIO_3_4, CropMode.RATIO_4_3, CropMode.RATIO_9_16, CropMode.RATIO_16_9
)
@Retention(AnnotationRetention.SOURCE)
annotation class CropMode {
    companion object {
        const val FREE = 0
        const val ORIGIN = 1
        const val RATIO_1_1 = 2
        const val RATIO_2_3 = 3
        const val RATIO_3_2 = 4
        const val RATIO_3_4 = 5
        const val RATIO_4_3 = 6
        const val RATIO_9_16 = 7
        const val RATIO_16_9 = 8
    }
}