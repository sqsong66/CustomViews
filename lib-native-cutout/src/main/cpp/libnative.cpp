#include <jni.h>
#include <string>
#include "LibAlpha.h"
#include <android/bitmap.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include "ImageProcessing.cpp"
#include <thread>

#define TAG "sqsong"
#define LOGE(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

#define MAKE_ABGR(a, b, g, r) (((a&0xff)<<24) | ((b & 0xff) << 16) | ((g & 0xff) << 8 ) | (r & 0xff))
#define BGR_8888_A(p) ((p & (0xff<<24))   >> 24 )
#define BGR_8888_B(p) ((p & (0xff << 16)) >> 16 )
#define BGR_8888_G(p) ((p & (0xff << 8))  >> 8 )
#define BGR_8888_R(p) (p & (0xff) )

jobject createBitmap(JNIEnv *env, jint width, jint height) {
    auto bitmapCls = env->FindClass("android/graphics/Bitmap");
    auto createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    // 声明 格式
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    auto getBitmapFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, getBitmapFunction, configName);
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, width, height, bitmapConfig);
    return newBitmap;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_mergeRGBA(JNIEnv *env, jobject thiz, jintArray dst, jintArray src, jintArray alpha, jint width, jint height) {
    if (dst == nullptr || src == nullptr || alpha == nullptr)
        return -10;
    auto *pdst = (uint8_t *) env->GetIntArrayElements(dst, nullptr);
    auto *psrc = (uint8_t *) env->GetIntArrayElements(src, nullptr);
    auto *palpha = (uint8_t *) env->GetIntArrayElements(alpha, nullptr);
    jint src_type = 4;
    jint src_stride = width * src_type;
    jint dst_stride = src_stride;
    jint alpha_type = 4;
    jint alpha_stride = width * alpha_type;

    return WXMergeRGBA(pdst, psrc, palpha, width, height, src_type, src_stride, dst_stride,
                       alpha_type, alpha_stride);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_enhanceForeground(JNIEnv *env, jobject thiz, jintArray dst, jintArray src, jint width, jint height,
                                                                  jintArray alpha, jintArray rect, jboolean is_mask_alpha_channel) {
    if (dst == nullptr || src == nullptr || alpha == nullptr)
        return -10;
    auto *pdst = (uint8_t *) env->GetIntArrayElements(dst, nullptr);
    auto *psrc = (uint8_t *) env->GetIntArrayElements(src, nullptr);
    auto *palpha = (uint8_t *) env->GetIntArrayElements(alpha, nullptr);
    int *prect = nullptr;
    if (rect != nullptr) {
        prect = (int *) env->GetIntArrayElements(rect, nullptr);
    }
    jint dst_type = 4;
    jint dst_stride = dst_type * width;
    jint src_type = 4;
    jint src_stride = src_type * width;
    jint alpha_type = 4;
    jint alpha_stride = alpha_type * width;
    int ret = WXEnhanceForeground(pdst, dst_type, dst_stride, psrc, width, height, src_type,
                                  src_stride, palpha, alpha_type, alpha_stride, prect, is_mask_alpha_channel);
    if (prect != nullptr) {
        env->SetIntArrayRegion(rect, 0, 4, prect);
    }
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_createShadow(JNIEnv *env, jobject thiz, jintArray rgba_view, jint view_width, jint view_height, jint x, jint y, jintArray rgba_fg, jint fg_width, jint fg_height, jbyte r, jbyte g, jbyte b) {
    if (rgba_view == nullptr || rgba_fg == nullptr)
        return -7;
    auto *prgba_view = (uint8_t *) env->GetIntArrayElements(rgba_view, nullptr);
    auto *prgba_fg = (uint8_t *) env->GetIntArrayElements(rgba_fg, nullptr);
    jint view_stride = view_width * 4;
    jint fg_stride = fg_width * 4;
    int ret = WXShadowView(prgba_view, view_stride, view_width, view_height, x, y, prgba_fg,
                           fg_stride, fg_width, fg_height, r, g, b);
    return ret;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_nativeCutout(JNIEnv *env, jobject thiz, jobject src_bitmap,
                                                             jobject mask_bitmap, jboolean cut_to_edge, jboolean is_mask_alpha_channel) {
    void *srcBitmapPointer = nullptr;
    void *maskBitmapPointer = nullptr;
    AndroidBitmapInfo bitmapInfo;
    AndroidBitmapInfo maskBitmapInfo;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, src_bitmap, &bitmapInfo)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo() src_bitmap failed! error = %d", ret);
        return nullptr;
    }
    if ((ret = AndroidBitmap_getInfo(env, mask_bitmap, &maskBitmapInfo)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo() src_bitmap failed! error = %d", ret);
        return nullptr;
    }
    if ((ret = AndroidBitmap_lockPixels(env, src_bitmap, &srcBitmapPointer)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels() src_bitmap failed! error = %d", ret);
        return nullptr;
    }

    if ((ret = AndroidBitmap_lockPixels(env, mask_bitmap, &maskBitmapPointer)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels() mask_bitmap failed! error = %d", ret);
    }

    uint32_t width = bitmapInfo.width;
    uint32_t height = bitmapInfo.height;
    auto uint8SrcPointer = (uint8_t *) srcBitmapPointer;
    auto uint8MaskPointer = (uint8_t *) maskBitmapPointer;
    int cutoutRect[] = {0, 0, static_cast<int>(width), static_cast<int>(height)}; // x, y, w, h

    auto start = std::chrono::steady_clock::now();
    // 前景优化
    ret = WXEnhanceForeground(uint8SrcPointer, 4, 4 * width, uint8SrcPointer, width, height, 4,
                              4 * width, uint8MaskPointer, 4, 4 * width, cutoutRect, is_mask_alpha_channel);
    auto end = std::chrono::steady_clock::now();

    LOGD("cutoutRect: [%d, %d, %d, %d], WXEnhanceForeground costTime: %lld ms", cutoutRect[0], cutoutRect[1], cutoutRect[2], cutoutRect[3],
         std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count());

    // 如果前景优化未处理成功边界数据，则将原图的宽高设置到对应的位置
    if (cutoutRect[2] == 0) cutoutRect[2] = static_cast<int>(width);
    if (cutoutRect[3] == 0) cutoutRect[3] = static_cast<int>(height);
    if (ret < 0) LOGE("Enhance foreground error: %d", ret);

    // 创建一个结果bitmap
    auto bitmapCls = env->FindClass("android/graphics/Bitmap");
    auto createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    auto getBitmapFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, getBitmapFunction, configName);
    int srcRect[] = {0, 0, static_cast<int>(width), static_cast<int>(height)}; // x, y, w, h
    int resultRect[] = {0, 0, static_cast<int>(width), static_cast<int>(height)}; // x, y, w, h
    if (cut_to_edge) { // 裁剪到边缘则使用抠图的尺寸
        resultRect[0] = cutoutRect[0];
        resultRect[1] = cutoutRect[1];
        resultRect[2] = cutoutRect[2];
        resultRect[3] = cutoutRect[3];
    }
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, resultRect[2], resultRect[3], bitmapConfig);
    void *resultBitmapPinter;
    // 获取到抠图bitmap的内存指针
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &resultBitmapPinter)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels() newBitmap failed! error = %d", ret);
        return nullptr;
    }

    auto uint32SrcPointer = (uint32_t *) srcBitmapPointer;
    auto uint32MaskPointer = (uint32_t *) maskBitmapPointer;
    auto uint32ResultPointer = (uint32_t *) resultBitmapPinter;
    for (uint i = 0; i < resultRect[3]; ++i) { // height
        for (uint j = 0; j < resultRect[2]; ++j) { // width
            uint index = (i + resultRect[1]) * width + resultRect[0] + j;
            int maskColor = uint32MaskPointer[index];
            int sourceColor = uint32SrcPointer[index];
            int a = BGR_8888_A(maskColor);
            int r = BGR_8888_R(maskColor);

            int b1 = BGR_8888_B(sourceColor);
            int g1 = BGR_8888_G(sourceColor);
            int r1 = BGR_8888_R(sourceColor);
            uint32ResultPointer[resultRect[2] * i + j] = MAKE_ABGR(is_mask_alpha_channel ? a : r, r1, g1, b1);
        }
    }

    jintArray resultArray = env->NewIntArray(resultRect[2] * resultRect[3]);
    env->SetIntArrayRegion(resultArray, 0, resultRect[2] * resultRect[3], reinterpret_cast<const jint *>(uint32ResultPointer));
    jmethodID setPixelsMid = env->GetMethodID(bitmapCls, "setPixels", "([IIIIIII)V");
    env->CallVoidMethod(newBitmap, setPixelsMid, resultArray, 0, resultRect[2], 0, 0, resultRect[2], resultRect[3]);

    // 创建上层需要的NativeCutoutResult对象
    jclass resultClass = env->FindClass("com/wangxutech/picwish/libnative/data/NativeCutoutResult");
    jmethodID jmethodId = env->GetMethodID(resultClass, "<init>", "(Landroid/graphics/Bitmap;[I[I)V");
    jintArray rectArray = env->NewIntArray(4);
    env->SetIntArrayRegion(rectArray, 0, 4, cutoutRect);
    jintArray srcArray = env->NewIntArray(4);
    env->SetIntArrayRegion(srcArray, 0, 4, srcRect);
    jobject resultObject = env->NewObject(resultClass, jmethodId, newBitmap, srcArray, rectArray);
    (*env).DeleteLocalRef(resultClass);

    AndroidBitmap_unlockPixels(env, src_bitmap);
    AndroidBitmap_unlockPixels(env, mask_bitmap);
    AndroidBitmap_unlockPixels(env, newBitmap);
    return resultObject;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_nativeCutout1(JNIEnv *env, jobject thiz, jobject src_bitmap,
                                                              jobject mask_bitmap, jboolean cut_to_edge, jboolean is_mask_alpha_channel) {
    AndroidBitmapInfo sourceInfo;
    AndroidBitmap_getInfo(env, src_bitmap, &sourceInfo);
    void *sourcePixels;
    AndroidBitmap_lockPixels(env, src_bitmap, &sourcePixels);

    AndroidBitmapInfo maskInfo;
    AndroidBitmap_getInfo(env, mask_bitmap, &maskInfo);
    void *maskPixels;
    AndroidBitmap_lockPixels(env, mask_bitmap, &maskPixels);

    int width = static_cast<int>(sourceInfo.width);
    int height = static_cast<int>(sourceInfo.height);

    // 创建 Bitmap 对象
    jobject bitmap = createBitmap(env, width, height);
    AndroidBitmapInfo bitmapInfo;
    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
    void *pixels;
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    auto *destPixelPtr = static_cast<uint32_t *>(pixels);

    auto *maskPixelPtr = static_cast<uint32_t *>(maskPixels);
    auto *pixelPtr = static_cast<uint32_t *>(sourcePixels);
    int min_x = width, min_y = height, max_x = 0, max_y = 0;
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int index = y * width + x;
            // 提取mask图像的alpha值。由于Android中不存在单通道图片，所以mask图中的alpha值存储在RGB通道上(RGB通道上的值是一样的)
            uint32_t maskPixel = maskPixelPtr[index];
            auto alpha = (maskPixel >> 16) & 0xFF;
            // 获取抠图图像边界
            if (alpha > 0) {
                if (y < min_y)
                    min_y = y;
                if (y > max_y)
                    max_y = y;
                if (x < min_x)
                    min_x = x;
                if (x > max_x)
                    max_x = x;
            }

            // 获取原图的rgb值
            uint32_t pixel = pixelPtr[index];
            uint32_t red = (pixel >> 16) & 0xFF;
            uint32_t green = (pixel >> 8) & 0xFF;
            uint32_t blue = pixel & 0xFF;
            // 将原图的RGB值与mask图ALPHA值进行预乘的到新图的ARGB值
            red = (red * alpha) / 255;
            green = (green * alpha) / 255;
            blue = (blue * alpha) / 255;
            // 重新组合像素值
            pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
            destPixelPtr[index] = pixel;
        }
    }
    LOGE("min_x: %d, min_y: %d, max_x: %d, max_y: %d\n", min_x, min_y, max_x, max_y);

    // 释放资源
    AndroidBitmap_unlockPixels(env, src_bitmap);
    AndroidBitmap_unlockPixels(env, mask_bitmap);
    AndroidBitmap_unlockPixels(env, bitmap);

    // 创建上层需要的NativeCutoutResult对象
    jclass resultClass = env->FindClass("com/wangxutech/picwish/libnative/data/NativeCutoutResult");
    jmethodID jmethodId = env->GetMethodID(resultClass, "<init>", "(Landroid/graphics/Bitmap;[I[I)V");
    jintArray srcArray = env->NewIntArray(4);
    env->SetIntArrayRegion(srcArray, 0, 4, new int[4]{0, 0, width, height});

    int rectArray[] = {0, 0, width, height};
    if (max_x > min_x && max_y > min_y) {
        rectArray[0] = min_x;
        rectArray[1] = min_y;
        rectArray[2] = max_x - min_x + 1;
        rectArray[3] = max_y - min_y + 1;
        LOGE("Cutout rectArray[%d, %d, %d, %d]\n", rectArray[0], rectArray[1], rectArray[2], rectArray[3]);

        jobject dstBitmap;
        if (cut_to_edge) {
            // 获取矩形区域数据
            jint cut_x = min_x;
            jint cut_y = min_y;
            jint cut_width = max_x - min_x + 1;
            jint cut_height = max_y - min_y + 1;

            // 创建目标Bitmap
            jclass bClass = env->FindClass("android/graphics/Bitmap");
            jmethodID createBMethod = env->GetStaticMethodID(bClass, "createBitmap", "(Landroid/graphics/Bitmap;IIIILandroid/graphics/Matrix;Z)Landroid/graphics/Bitmap;");
            jobject matrix = nullptr;
            dstBitmap = env->CallStaticObjectMethod(bClass, createBMethod, bitmap, cut_x, cut_y, cut_width, cut_height, matrix, JNI_TRUE);
            env->DeleteLocalRef(bitmap);
        } else {
            dstBitmap = bitmap;
        }
        jintArray destArray = env->NewIntArray(4);
        env->SetIntArrayRegion(destArray, 0, 4, rectArray);
        return env->NewObject(resultClass, jmethodId, dstBitmap, srcArray, destArray);
    } else {
        LOGE("Cutout rectArray[%d, %d, %d, %d]\n", rectArray[0], rectArray[1], rectArray[2], rectArray[3]);
        return env->NewObject(resultClass, jmethodId, bitmap, srcArray, srcArray);
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_createColorBitmapFromMask(JNIEnv *env, jobject thiz, jobject mask_bitmap, jint r, jint g, jint b) {
    void *maskPtr = nullptr;
    AndroidBitmapInfo bitmapInfo;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, mask_bitmap, &bitmapInfo)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_getInfo() src_bitmap failed! error = %d", ret);
        return nullptr;
    }

    if ((ret = AndroidBitmap_lockPixels(env, mask_bitmap, &maskPtr)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() mask_bitmap failed! error = %d", ret);
        return nullptr;
    }

    uint32_t width = bitmapInfo.width;
    uint32_t height = bitmapInfo.height;
    // 创建一个新的裁剪到边缘的抠图bitmap
    jobject newBitmap = createBitmap(env, width, height);
    void *colorBitmapPixels;
    // 获取到抠图bitmap的内存指针
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &colorBitmapPixels)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() newBitmap failed! error = %d", ret);
        return nullptr;
    }

    // 获取到原图bitmap的像素指针
    auto pixelArr = (uint8_t *) maskPtr;
    auto colorArr = (uint8_t *) colorBitmapPixels;
    int srcIndex = 0, rowIndex = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int a = pixelArr[srcIndex];
            colorArr[srcIndex] = r * a / 255; // r
            colorArr[srcIndex + 1] = g * a / 255; // g
            colorArr[srcIndex + 2] = b * a / 255; // b
            colorArr[srcIndex + 3] = 255 * a / 255; // a

            srcIndex += 4;
        }
        rowIndex += width * 4;
        srcIndex = rowIndex;
    }

    AndroidBitmap_unlockPixels(env, newBitmap);
    AndroidBitmap_unlockPixels(env, mask_bitmap);
    return newBitmap;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_convertAlphaMaskBitmap(JNIEnv *env, jobject thiz, jobject paint_bitmap) {
    void *maskPtr = nullptr;
    AndroidBitmapInfo bitmapInfo;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, paint_bitmap, &bitmapInfo)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_getInfo() src_bitmap failed! error = %d", ret);
        return nullptr;
    }

    if ((ret = AndroidBitmap_lockPixels(env, paint_bitmap, &maskPtr)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() mask_bitmap failed! error = %d", ret);
        return nullptr;
    }

    uint32_t width = bitmapInfo.width;
    uint32_t height = bitmapInfo.height;
    // 创建一个新的裁剪到边缘的抠图bitmap
    jobject newBitmap = createBitmap(env, width, height);
    void *colorBitmapPixels;
    // 获取到抠图bitmap的内存指针
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &colorBitmapPixels)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() newBitmap failed! error = %d", ret);
        return nullptr;
    }

    // 获取到原图bitmap的像素指针
    auto pixelArr = (uint8_t *) maskPtr;
    auto colorArr = (uint8_t *) colorBitmapPixels;
    int srcIndex = 0, rowIndex = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            // 获取到透明mask图的alpha，将其赋值给目标图片的r/g/b通道，目标通道的alpha值设置为255，即变换为黑白mask图
            int alpha = pixelArr[srcIndex + 3];
            colorArr[srcIndex] = alpha; // r
            colorArr[srcIndex + 1] = alpha; // g
            colorArr[srcIndex + 2] = alpha; // b
            colorArr[srcIndex + 3] = 255; // a

            srcIndex += 4;
        }
        rowIndex += width * 4;
        srcIndex = rowIndex;
    }

    AndroidBitmap_unlockPixels(env, newBitmap);
    AndroidBitmap_unlockPixels(env, paint_bitmap);
    return newBitmap;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_adjustBitmap(JNIEnv *jenv, jobject clazz, jobject src) {
    unsigned char *srcByteBuffer;
    int result = 0;
    int i, j;
    AndroidBitmapInfo srcInfo;

    result = AndroidBitmap_getInfo(jenv, src, &srcInfo);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }

    result = AndroidBitmap_lockPixels(jenv, src, (void **) &srcByteBuffer);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }

    int width = srcInfo.width;
    int height = srcInfo.height;
    glReadPixels(0, 0, srcInfo.width, srcInfo.height, GL_RGBA, GL_UNSIGNED_BYTE, srcByteBuffer);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            uint32_t c = width * y + x;
            uint8_t a = srcByteBuffer[c * 4 + 3];
            float aNormalized = a / 255.0f;
            srcByteBuffer[c * 4] = (uint32_t) std::min(srcByteBuffer[c * 4] * aNormalized, 255.0f);
            srcByteBuffer[c * 4 + 1] = (uint32_t) std::min(srcByteBuffer[c * 4 + 1] * aNormalized, 255.0f);
            srcByteBuffer[c * 4 + 2] = (uint32_t) std::min(srcByteBuffer[c * 4 + 2] * aNormalized, 255.0f);
        }
    }

    uint32_t *pIntBuffer = (uint32_t *) srcByteBuffer;
    for (i = 0; i < height / 2; i++) {
        for (j = 0; j < width; j++) {
            int temp = pIntBuffer[(height - i - 1) * width + j];
            pIntBuffer[(height - i - 1) * width + j] = pIntBuffer[i * width + j];
            pIntBuffer[i * width + j] = temp;
        }
    }
    AndroidBitmap_unlockPixels(jenv, src);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_getGlBitmap(JNIEnv *env, jobject thiz, jint width, jint height) {
    auto bitmapCls = env->FindClass("android/graphics/Bitmap");
    auto createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    auto getBitmapFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, getBitmapFunction, configName);
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, width, height, bitmapConfig);

    int ret;
    void *resultBitmapPinter;
    // 获取到抠图bitmap的内存指针
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &resultBitmapPinter)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels() newBitmap failed! error = %d", ret);
        return nullptr;
    }

    glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, resultBitmapPinter);

    auto uint32ResultPointer = (uint32_t *) resultBitmapPinter;
    // 开辟一个新的数组来装重新排列后的Bitmap像素数据
    auto *newBitmapPixels = new uint32_t[width * height];
    int temp = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            int color = uint32ResultPointer[temp++];
            int a = BGR_8888_A(color);
            int r = BGR_8888_R(color);
            int g = BGR_8888_G(color);
            int b = BGR_8888_B(color);
            // 由于从OpenGL中读取处理的图片是倒立的，所以需要将其像素数据位置上下倒置一下
            int index = width * (height - 1 - i) + j;
            newBitmapPixels[index] = MAKE_ABGR(a, r, g, b);
        }
    }
    // 通过Bitmap的setPixels将处理好的像素数据重新填充到Bitmap中去
    jintArray resultArray = env->NewIntArray(width * height);
    env->SetIntArrayRegion(resultArray, 0, width * height, reinterpret_cast<const jint *>(newBitmapPixels));
    jmethodID setPixelsMid = env->GetMethodID(bitmapCls, "setPixels", "([IIIIIII)V");
    env->CallVoidMethod(newBitmap, setPixelsMid, resultArray, 0, width, 0, 0, width, height);

    delete[] newBitmapPixels;
    AndroidBitmap_unlockPixels(env, newBitmap);
    return newBitmap;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_unPremultipliedBitmap(JNIEnv *jenv, jobject thiz, jobject src) {
    unsigned char *srcByteBuffer;
    int result = 0;
    int i, j;
    AndroidBitmapInfo srcInfo;

    result = AndroidBitmap_getInfo(jenv, src, &srcInfo);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }

    result = AndroidBitmap_lockPixels(jenv, src, (void **) &srcByteBuffer);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        return;
    }

    int width = srcInfo.width;
    int height = srcInfo.height;
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            uint32_t c = width * y + x;
            uint8_t a = srcByteBuffer[c * 4 + 3];
            if (a != 0 && a != 255) { // alpha为0或255的不处理(节省时间)
                float aNormalized = a / 255.0f;
                srcByteBuffer[c * 4] = (uint32_t) std::min(srcByteBuffer[c * 4] / aNormalized, 255.0f);
                srcByteBuffer[c * 4 + 1] = (uint32_t) std::min(srcByteBuffer[c * 4 + 1] / aNormalized, 255.0f);
                srcByteBuffer[c * 4 + 2] = (uint32_t) std::min(srcByteBuffer[c * 4 + 2] / aNormalized, 255.0f);
            }
        }
    }
    AndroidBitmap_unlockPixels(jenv, src);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_lightOn(JNIEnv *env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return nullptr;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        return nullptr;
    }
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return nullptr;
    }

    cv::Mat src(info.height, info.width, CV_8UC4, pixels);
    cv::Mat dst(info.height, info.width, CV_8UC4);

    // 使用多线程处理图像
    int num_threads = std::thread::hardware_concurrency();
    std::vector<std::thread> threads;
    int rows_per_thread = src.rows / num_threads;
    for (int i = 0; i < num_threads; ++i) {
        int start_row = i * rows_per_thread;
        int end_row = (i == num_threads - 1) ? src.rows : start_row + rows_per_thread;
        threads.emplace_back(process_image, std::ref(src), std::ref(dst), start_row, end_row);
    }

    for (auto &thread: threads) {
        thread.join();
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    // Get the Bitmap class and createBitmap method ID
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(
            bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    // Get the Bitmap.Config.ARGB_8888 field ID and value
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888FieldID = env->GetStaticFieldID(bitmapConfigClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Config = env->GetStaticObjectField(bitmapConfigClass, argb8888FieldID);

    // Create a new Bitmap object
    jobject newBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, info.width, info.height, argb8888Config);

    // Lock the pixels of the new Bitmap and copy the processed data into it
    void *newPixels;
    if (AndroidBitmap_lockPixels(env, newBitmap, &newPixels) < 0) {
        return nullptr;
    }
    memcpy(newPixels, dst.data, dst.total() * dst.elemSize());
    AndroidBitmap_unlockPixels(env, newBitmap);

    return newBitmap;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_premultiplyAlpha(JNIEnv *env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    // 获取Bitmap信息
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        return;
    }

    // 锁定Bitmap以获取像素数据
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return;
    }

    // 预乘alpha处理
    uint32_t *line = (uint32_t *) pixels;
    for (int y = 0; y < info.height; ++y) {
        for (int x = 0; x < info.width; ++x) {
            uint32_t *pixel = line + x;
            uint8_t alpha = (*pixel >> 24) & 0xFF;
            uint8_t red = (*pixel >> 16) & 0xFF;
            uint8_t green = (*pixel >> 8) & 0xFF;
            uint8_t blue = *pixel & 0xFF;

            red = (red * alpha) / 255;
            green = (green * alpha) / 255;
            blue = (blue * alpha) / 255;

            *pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        line = (uint32_t *) ((char *) line + info.stride);
    }

    // 解锁Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);
}

/*jobject createBitmap(JNIEnv *env, jint width, jint height) {
    auto bitmapCls = env->FindClass("android/graphics/Bitmap");
    auto createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    // 声明 格式
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    auto getBitmapFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, getBitmapFunction, configName);
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, width, height, bitmapConfig);
    return newBitmap;
}*/

extern "C"
JNIEXPORT jobject JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_convertMaskToAlphaChannel(JNIEnv *env, jobject thiz, jobject mask_bitmap) {
    void *maskPtr = nullptr;
    AndroidBitmapInfo bitmapInfo;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, mask_bitmap, &bitmapInfo)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_getInfo() src_bitmap failed! error = %d", ret);
        return nullptr;
    }

    if ((ret = AndroidBitmap_lockPixels(env, mask_bitmap, &maskPtr)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() mask_bitmap failed! error = %d", ret);
        return nullptr;
    }

    uint32_t width = bitmapInfo.width;
    uint32_t height = bitmapInfo.height;
    // 创建一个新的裁剪到边缘的抠图bitmap
    jobject newBitmap = createBitmap(env, width, height);
    void *colorBitmapPixels;
    // 获取到抠图bitmap的内存指针
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &colorBitmapPixels)) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("createColorBitmapFromMask AndroidBitmap_lockPixels() newBitmap failed! error = %d", ret);
        return nullptr;
    }

    // 获取到原图bitmap的像素指针
    auto pixelArr = (uint8_t *) maskPtr;
    auto colorArr = (uint8_t *) colorBitmapPixels;
    int srcIndex = 0, rowIndex = 0;
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            // mask图的r/g/b值是相同的，取其中一个通道的值来判断alpha值，
            int alpha = pixelArr[srcIndex];
            int a;
            if (alpha == 255) { // 如果像素是白色，则alpha值为0，让其变成全透明
                a = 0;
            } else if (alpha == 0) { // 如果像素是黑色，则alpha值为255，让其变成全不透明
                a = 255;
            } else {
                a = 255 - alpha; // 其他颜色则根据alpha值来计算透明度
            }
            colorArr[srcIndex] = 0; // r
            colorArr[srcIndex + 1] = 0; // g
            colorArr[srcIndex + 2] = 0; // b
            colorArr[srcIndex + 3] = a; // a

            srcIndex += 4;
        }
        rowIndex += width * 4;
        srcIndex = rowIndex;
    }

    AndroidBitmap_unlockPixels(env, newBitmap);
    AndroidBitmap_unlockPixels(env, mask_bitmap);
    return newBitmap;
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_cropPNGImageBitmap(JNIEnv *env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo bitmapInfo;
    void *pixels;

    // 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0) {
        return nullptr; // 获取失败
    }

    // 锁定 Bitmap 以获取其像素数据
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return nullptr; // 锁定失败
    }

    int width = bitmapInfo.width;
    int height = bitmapInfo.height;
    auto *pixelData = (uint32_t *) pixels;

    int minX = width;
    int minY = height;
    int maxX = -1;
    int maxY = -1;

    // 遍历像素数据以查找非透明区域
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            uint32_t pixel = pixelData[y * width + x];
            uint8_t alpha = (pixel >> 24) & 0xFF; // 获取 alpha 通道
            if (alpha != 0) {
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
    }

    // 解锁 Bitmap
    AndroidBitmap_unlockPixels(env, bitmap);

    // 如果没有找到任何非透明区域，返回整个图片的宽高，起始坐标为 (0, 0)
    jintArray result = env->NewIntArray(4);
    if (maxX == -1 || maxY == -1) {
        jint bounds[] = {0, 0, width, height};
        env->SetIntArrayRegion(result, 0, 4, bounds);
    } else {
        jint bounds[] = {minX, minY, maxX - minX + 1, maxY - minY + 1};
        env->SetIntArrayRegion(result, 0, 4, bounds);
    }
    return result;
}