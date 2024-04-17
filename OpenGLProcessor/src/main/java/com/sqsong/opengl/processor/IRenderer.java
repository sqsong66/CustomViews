package com.sqsong.opengl.processor;


/**
 * Created by yuxfzju on 2017/2/10.
 */
interface IRenderer<T> {

    void onDrawFrame(T t, Boolean offScreen);

}
