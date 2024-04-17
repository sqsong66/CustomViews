package com.sqsong.opengl.processor.anno;

import static com.sqsong.opengl.processor.anno.Mode.MODE_BOX;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yuxfzju on 2017/2/9.
 */

@IntDef({MODE_BOX, Mode.MODE_GAUSSIAN, Mode.MODE_STACK})
@Retention(RetentionPolicy.SOURCE)
public @interface Mode {

    int MODE_BOX = 0;
    int MODE_GAUSSIAN = 1;
    int MODE_STACK = 2;

}