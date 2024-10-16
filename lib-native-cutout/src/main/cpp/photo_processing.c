/*
 * Copyright (C) 2012 Lightbox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <stdlib.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <android/bitmap.h>
#include "beauty.h"

#define  LOG_TAG    "IMAGE_EDIT_PROCESSING"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

JNIEXPORT void JNICALL
Java_com_wangxutech_picwish_libnative_NativeLib_handleSmoothAndWhiteSkin(JNIEnv *env, jclass clazz, jobject bitmap, jfloat smoothValue, jfloat whiteValue) {
    AndroidBitmapInfo info;
    void *pixels;
    int ret;

    if (bitmap == NULL)
        return;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGI("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGI("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    LOGI("Bitmap smooth and whiteSkin handle,smoothValue = %f and whiteSkinValue = %f", smoothValue, whiteValue);
    initBeautyMatrix((uint32_t *) pixels, info.width, info.height);

    setSmooth((uint32_t *) pixels, smoothValue, info.width, info.height);

    initBeautyMatrix((uint32_t *) pixels, info.width, info.height);

    setWhiteSkin((uint32_t *) pixels, whiteValue, info.width, info.height);

    AndroidBitmap_unlockPixels(env, bitmap);

    //free memory code
    freeMatrix();
}

JNIEXPORT void JNICALL
Java_io_github_treech_PhotoProcess_handleSmooth(JNIEnv *env, jobject obj, jobject bitmap,
                                                jfloat smoothValue) {
    AndroidBitmapInfo info;
    void *pixels;
    int ret;

    if (bitmap == NULL)
        return;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGI("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGI("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    LOGI("AndroidBitmap_smooth handle");

    initBeautyMatrix((uint32_t *) pixels, info.width, info.height);
    setSmooth((uint32_t *) pixels, smoothValue, info.width, info.height);

    AndroidBitmap_unlockPixels(env, bitmap);

    //free memory code
    freeMatrix();
}

JNIEXPORT void JNICALL
Java_io_github_treech_PhotoProcess_handleWhiteSkin(JNIEnv *env, jobject obj, jobject bitmap,
                                                   jfloat whiteValue) {
    AndroidBitmapInfo info;
    void *pixels;
    int ret;

    if (bitmap == NULL)
        return;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGI("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGI("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    LOGI("AndroidBitmap_whiteSkin handle");
    initBeautyMatrix((uint32_t *) pixels, info.width, info.height);

    LOGI("AndroidBitmap_whiteSkin whiteValue = %f", whiteValue);
    setWhiteSkin((uint32_t *) pixels, whiteValue, info.width, info.height);

    AndroidBitmap_unlockPixels(env, bitmap);

    //free memory
    freeMatrix();
}

void setWhiteSkin(uint32_t *pix, float whiteVal, int width, int height) {
    if (whiteVal >= 1.0 && whiteVal <= 10.0) { //1.0~10.0
        float a = logf(whiteVal);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int offset = i * width + j;
                ARGB RGB;
                convertIntToArgb(mImageData_rgb[offset], &RGB);
                if (a != 0) {
                    RGB.red = 255 * (logf(div255(RGB.red) * (whiteVal - 1) + 1) / a);
                    RGB.green = 255 * (logf(div255(RGB.green) * (whiteVal - 1) + 1) / a);
                    RGB.blue = 255 * (logf(div255(RGB.blue) * (whiteVal - 1) + 1) / a);
                    if (RGB.alpha != 255) RGB.alpha = 255 * (logf(div255(RGB.alpha) * (whiteVal - 1) + 1) / a);
                }
                pix[offset] = convertArgbToInt(RGB);
            }
        }
    }//end if
}

void setSmooth(uint32_t *pix, float smoothValue, int width, int height) {//磨皮操作
    if (mIntegralMatrix == NULL || mIntegralMatrixSqr == NULL || mSkinMatrix == NULL) {//预操作辅助未准备好
        LOGE("not init correctly");
        return;
    }

    LOGE("AndroidBitmap_smooth setSmooth start---- smoothValue = %f", smoothValue);

    RGBToYCbCr((uint8_t *) mImageData_rgb, mImageData_yuv, width * height);

    int radius = width > height ? width * 0.02 : height * 0.02;

    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            int offset = i * width + j;
            if (mSkinMatrix[offset] == 255) {
                int iMax = i + radius >= height - 1 ? height - 1 : i + radius;
                int jMax = j + radius >= width - 1 ? width - 1 : j + radius;
                int iMin = i - radius <= 1 ? 1 : i - radius;
                int jMin = j - radius <= 1 ? 1 : j - radius;

                int squar = (iMax - iMin + 1) * (jMax - jMin + 1);
                int i4 = iMax * width + jMax;
                int i3 = (iMin - 1) * width + (jMin - 1);
                int i2 = iMax * width + (jMin - 1);
                int i1 = (iMin - 1) * width + jMax;

                float m = (mIntegralMatrix[i4] + mIntegralMatrix[i3] - mIntegralMatrix[i2] - mIntegralMatrix[i1]) / squar;
                float v = (mIntegralMatrixSqr[i4] + mIntegralMatrixSqr[i3] - mIntegralMatrixSqr[i2] - mIntegralMatrixSqr[i1]) / squar - m * m;
                float k = v / (v + smoothValue);

                mImageData_yuv[offset * 3] = ceilf(m - k * m + k * mImageData_yuv[offset * 3]);
            }
        }
    }
    YCbCrToRGB(mImageData_yuv, (uint8_t *) pix, width * height);

    LOGI("AndroidBitmap_smooth setSmooth END!----");
}

void freeMatrix() {
    if (mIntegralMatrix != NULL) {
        free(mIntegralMatrix);
        mIntegralMatrix = NULL;
    }

    if (mIntegralMatrixSqr != NULL) {
        free(mIntegralMatrixSqr);
        mIntegralMatrixSqr = NULL;
    }

    if (mSkinMatrix != NULL) {
        free(mSkinMatrix);
        mSkinMatrix = NULL;
    }

    if (mImageData_rgb != NULL) {
        free(mImageData_rgb);
        mImageData_rgb = NULL;
    }

    if (mImageData_yuv != NULL) {
        free(mImageData_yuv);
        mImageData_yuv = NULL;
    }
}

void initBeautyMatrix(uint32_t *pix, int width, int height) {

    if (mImageData_rgb == NULL)
        mImageData_rgb = (uint32_t *) malloc(sizeof(uint32_t) * width * height);

    memcpy(mImageData_rgb, pix, sizeof(uint32_t) * width * height);

    if (mImageData_yuv == NULL)
        mImageData_yuv = (uint8_t *) malloc(sizeof(uint8_t) * width * height * 4);

    RGBToYCbCr((uint8_t *) mImageData_rgb, mImageData_yuv, width * height);

    initSkinMatrix(pix, width, height);
    initIntegralMatrix(width, height);
}

void initSkinMatrix(uint32_t *pix, int w, int h) {
    LOGE("start - initSkinMatrix");
    if (mSkinMatrix == NULL)
        mSkinMatrix = (uint8_t *) malloc(sizeof(uint8_t) * w * h);
    //mSkinMatrix = new uint8_t[w * h];

    for (int i = 0; i < h; i++) {
        for (int j = 0; j < w; j++) {
            int offset = i * w + j;
            ARGB RGB;
            convertIntToArgb(pix[offset], &RGB);
            if ((RGB.blue > 95 && RGB.green > 40 && RGB.red > 20 &&
                 RGB.blue - RGB.red > 15 && RGB.blue - RGB.green > 15) || //uniform illumination
                (RGB.blue > 200 && RGB.green > 210 && RGB.red > 170 &&
                 abs(RGB.blue - RGB.red) <= 15 && RGB.blue > RGB.red &&
                 RGB.green > RGB.red)) //lateral illumination
                mSkinMatrix[offset] = 255;
            else
                mSkinMatrix[offset] = 0;
        }
    }
    LOGE("end - initSkinMatrix");
}

void initIntegralMatrix(int width, int height) {
    LOGI("initIntegral");
    LOGI("width = %d height = %d", width, height);

    if (mIntegralMatrix == NULL)
        mIntegralMatrix = (uint64_t *) malloc(sizeof(uint64_t) * width * height);
    //mIntegralMatrix = new uint64_t[width * height];
    if (mIntegralMatrixSqr == NULL)
        mIntegralMatrixSqr = (uint64_t *) malloc(sizeof(uint64_t) * width * height);
    //mIntegralMatrixSqr = new uint64_t[width * height];

    LOGI("malloc complete");

    //uint64_t *columnSum = new uint64_t[width];
    uint64_t *columnSum = (uint64_t *) malloc(sizeof(uint64_t) * width);
    //uint64_t *columnSumSqr = new uint64_t[width];
    uint64_t *columnSumSqr = (uint64_t *) malloc(sizeof(uint64_t) * width);

    columnSum[0] = mImageData_yuv[0];
    columnSumSqr[0] = mImageData_yuv[0] * mImageData_yuv[0];

    mIntegralMatrix[0] = columnSum[0];
    mIntegralMatrixSqr[0] = columnSumSqr[0];

    for (int i = 1; i < width; i++) {

        columnSum[i] = mImageData_yuv[3 * i];
        columnSumSqr[i] = mImageData_yuv[3 * i] * mImageData_yuv[3 * i];

        mIntegralMatrix[i] = columnSum[i];
        mIntegralMatrix[i] += mIntegralMatrix[i - 1];
        mIntegralMatrixSqr[i] = columnSumSqr[i];
        mIntegralMatrixSqr[i] += mIntegralMatrixSqr[i - 1];
    }

    for (int i = 1; i < height; i++) {
        int offset = i * width;

        columnSum[0] += mImageData_yuv[3 * offset];
        columnSumSqr[0] += mImageData_yuv[3 * offset] * mImageData_yuv[3 * offset];

        mIntegralMatrix[offset] = columnSum[0];
        mIntegralMatrixSqr[offset] = columnSumSqr[0];

        for (int j = 1; j < width; j++) {
            columnSum[j] += mImageData_yuv[3 * (offset + j)];
            columnSumSqr[j] += mImageData_yuv[3 * (offset + j)] * mImageData_yuv[3 * (offset + j)];

            mIntegralMatrix[offset + j] = mIntegralMatrix[offset + j - 1] + columnSum[j];
            mIntegralMatrixSqr[offset + j] = mIntegralMatrixSqr[offset + j - 1] + columnSumSqr[j];
        }
    }

    free(columnSum);
    free(columnSumSqr);
    LOGI("initIntegral~end");
}

int32_t convertArgbToInt(ARGB argb) {
    return (argb.alpha << 24) | (argb.red << 16) | (argb.green << 8) | argb.blue;
}

void YCbCrToRGB(uint8_t *From, uint8_t *To, int length) {
    if (length < 1) return;
    int Red, Green, Blue, alpha;
    int Y, Cb, Cr, Ca;
    int i, offset;
    for (i = 0; i < length; i++) {
        offset = (i << 1) + i;
        Y = From[offset];
        Cb = From[offset + 1] - 128;
        Cr = From[offset + 2] - 128;
//        Ca = From[offset+3] - 128;
        Red = Y + ((RGBRCrI * Cr + HalfShiftValue) >> Shift);
        Green = Y + ((RGBGCbI * Cb + RGBGCrI * Cr + HalfShiftValue) >> Shift);
        Blue = Y + ((RGBBCbI * Cb + HalfShiftValue) >> Shift);
//        alpha = Y + ((RGBBCbI * Ca + HalfShiftValue) >> Shift);

        if (Red > 255) Red = 255; else if (Red < 0) Red = 0;
        if (Green > 255) Green = 255; else if (Green < 0) Green = 0;
        if (Blue > 255) Blue = 255; else if (Blue < 0) Blue = 0;
//        if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;
        offset = i << 2;

        To[offset] = (uint8_t) Blue;
        To[offset + 1] = (uint8_t) Green;
        To[offset + 2] = (uint8_t) Red;
//        To[offset+3] = alpha;
    }
}

void RGBToYCbCr(const uint8_t *From, uint8_t *To, int length) {
    if (length < 1) return;
    int Red, Green, Blue, alpha;
    int i, offset;
    for (i = 0; i < length; i++) {
        offset = i << 2;
        Blue = From[offset];
        Green = From[offset + 1];
        Red = From[offset + 2];
        alpha = From[offset + 3];

        offset = (i << 1) + i;
        To[offset] = (uint8_t) ((YCbCrYRI * Red + YCbCrYGI * Green + YCbCrYBI * Blue + HalfShiftValue) >> Shift);
        To[offset + 1] = (uint8_t) (128 + ((YCbCrCbRI * Red + YCbCrCbGI * Green + YCbCrCbBI * Blue + HalfShiftValue) >> Shift));
        To[offset + 2] = (uint8_t) (128 + ((YCbCrCrRI * Red + YCbCrCrGI * Green + YCbCrCrBI * Blue + HalfShiftValue) >> Shift));
        To[offset + 3] = alpha;
    }
}

void convertIntToArgb(uint32_t pixel, ARGB *argb) {
    argb->red = ((pixel >> 16) & 0xff);
    argb->green = ((pixel >> 8) & 0xff);
    argb->blue = (pixel & 0xff);
    argb->alpha = (pixel >> 24);
}
