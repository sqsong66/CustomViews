#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/bitmap.h>
#include <cmath>
#include <algorithm>
#include <array>

// 手动实现clamp函数
template<typename T>
T clamp(T value, T min, T max) {
    return std::max(min, std::min(value, max));
}

const std::array<double, 3> PG_LUMINANCE = {0.2126, 0.7152, 0.0722};

double dot(const std::array<double, 3> &rgb1, const std::array<double, 3> &rgb2) {
    return rgb1[0] * rgb2[0] + rgb1[1] * rgb2[1] + rgb2[2] * rgb2[2];
}

std::array<double, 3> pg_srgb_to_linear(const std::array<double, 3> &color_rgb) {
    std::array<double, 3> linear_rgb;
    for (size_t i = 0; i < 3; ++i) {
        double a = color_rgb[i] / 12.92;
        double b = std::pow((color_rgb[i] + 0.055) / 1.055, 2.4);
        linear_rgb[i] = (color_rgb[i] >= 0.04045) ? b : a;
    }
    return linear_rgb;
}

std::array<double, 3> pg_linear_to_srgb(const std::array<double, 3> &color_rgb) {
    std::array<double, 3> srgb_rgb;
    for (size_t i = 0; i < 3; ++i) {
        double a = 12.92 * color_rgb[i];
        double b = 1.055 * std::pow(color_rgb[i], 1.0 / 2.4) - 0.055;
        srgb_rgb[i] = (color_rgb[i] >= 0.0031308) ? b : a;
    }
    return srgb_rgb;
}

std::array<double, 4> pg_srgb_to_linear_alpha(const std::array<double, 4> &color) {
    auto linear_rgb = pg_srgb_to_linear({color[0], color[1], color[2]});
    return {linear_rgb[0], linear_rgb[1], linear_rgb[2], color[3]};
}

std::array<double, 4> pg_linear_to_srgb_alpha(const std::array<double, 4> &color) {
    auto srgb_rgb = pg_linear_to_srgb({color[0], color[1], color[2]});
    return {srgb_rgb[0], srgb_rgb[1], srgb_rgb[2], color[3]};
}

std::array<double, 4> pg_exposure_kernel(const std::array<double, 4> &color, double exposure) {
    std::array<double, 4> result_rgba;
    for (size_t i = 0; i < 4; ++i) {
        result_rgba[i] = (i == 3) ? color[i] : clamp(color[i] * std::pow(2.0, exposure), 0.0, 1.0);
    }
    return result_rgba;
}

double pg_highlights_shadows_multiplier(double l, double highlights, double shadows) {
    const double SHADOWS_L = 0.0;
    const double SHADOWS_RADIUS = 0.15;
    const double SHADOWS_AMPL = 1.0;
    const double HIGHLIGHTS_L = 1.0;
    const double HIGHLIGHTS_RADIUS = 0.4;
    const double HIGHLIGHTS_AMPL = 0.55;

    double shadows_multiplier = SHADOWS_AMPL * std::exp(-0.5 * std::pow((l - SHADOWS_L) / SHADOWS_RADIUS, 2.0));
    double highlights_multiplier = HIGHLIGHTS_AMPL * std::exp(-0.5 * std::pow((l - HIGHLIGHTS_L) / HIGHLIGHTS_RADIUS, 2.0));
    return 1.0 + highlights * highlights_multiplier + shadows * shadows_multiplier;
}

std::array<double, 4> pg_highlights_shadows_kernel(const std::array<double, 4> &color, double highlights, double shadows) {
    double luminance = dot({color[0], color[1], color[2]}, PG_LUMINANCE);
    double factor = pg_highlights_shadows_multiplier(luminance, highlights, shadows);
    std::array<double, 4> result_rgba;
    for (size_t i = 0; i < 4; ++i) {
        result_rgba[i] = (i == 3) ? color[i] : clamp(color[i] * factor, 0.0, 1.0);
    }
    return result_rgba;
}

std::array<double, 4> pg_saturation_kernel(const std::array<double, 4> &color, double saturation) {
    double luminance = dot({color[0], color[1], color[2]}, PG_LUMINANCE);
    std::array<double, 4> result_rgba;
    for (size_t i = 0; i < 4; ++i) {
        result_rgba[i] = (i == 3) ? color[i] : clamp((1.0 - saturation) * luminance + saturation * color[i], 0.0, 1.0);
    }
    return result_rgba;
}

std::array<double, 4> light_on_kernel(const std::array<double, 4> &color) {
    auto color_linear = pg_srgb_to_linear_alpha(color);
    color_linear = pg_exposure_kernel(color_linear, 0.375);
    color_linear = pg_highlights_shadows_kernel(color_linear, 0.1, 0.1);
    color_linear = pg_saturation_kernel(color_linear, 1.1);
    return pg_linear_to_srgb_alpha(color_linear);
}

void process_image(cv::Mat &src, cv::Mat &dst, int start_row, int end_row) {
    for (int y = start_row; y < end_row; ++y) {
        for (int x = 0; x < src.cols; ++x) {
            cv::Vec4b pixel = src.at<cv::Vec4b>(y, x);
            std::array<double, 4> color = {
                    pixel[0] / 255.0,
                    pixel[1] / 255.0,
                    pixel[2] / 255.0,
                    pixel[3] / 255.0
            };
            auto new_color = light_on_kernel(color);
            dst.at<cv::Vec4b>(y, x) = cv::Vec4b(
                    new_color[0] * 255.0,
                    new_color[1] * 255.0,
                    new_color[2] * 255.0,
                    new_color[3] * 255.0
            );
        }
    }
}