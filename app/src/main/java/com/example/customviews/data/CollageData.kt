package com.example.customviews.data

data class CollageData(
    val collageType: String,
    val solutions: List<Solution>
) {
    override fun toString(): String {
        return "CollageData(collageType='$collageType', solutions=${solutions.size})"
    }
}

data class Solution(
    val id: String,
    val type: String,
    val thumb: String,
    val width: Int,
    val height: Int? = null,
    val pictures: List<Picture>,
    val pictureAreas: List<PictureArea>
)

data class Picture(
    val id: String,
    val type: String,
    val src: String,
    val fillPolygon: Boolean,
    val alpha: Int,
    val rotate: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val width: Int,
    val height: Int,
    val hint: String? = null
) {
    fun isMask() = type.equals("mask", true)
}

data class PictureArea(
    val id: String,
    val type: String,
    val src: String,
    val backgroundColor: String,
    val alpha: Int,
    val paddingLeft: Int,
    val paddingRight: Int,
    val paddingTop: Int,
    val paddingBottom: Int,
    val spacing: Int
)
