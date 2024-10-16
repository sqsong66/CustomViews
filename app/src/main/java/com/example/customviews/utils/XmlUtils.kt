package com.example.customviews.utils

import android.content.Context
import com.example.customviews.data.Picture
import com.example.customviews.data.PictureArea
import com.example.customviews.data.Solution
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

fun parseCollages(context: Context): List<Solution> {
    val inputStream = context.assets.open("collage/resources.xml")
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(inputStream, "UTF-8")

    val solutions = mutableListOf<Solution>()
    var eventType = parser.eventType
    var currentTag: String?

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                if (currentTag == "item") {
                    val src = parser.getAttributeValue(null, "src")
                    val solution = parseSolutionXml(context, src)
                    solution?.let { solutions.add(it) }
                }
            }
        }
        eventType = parser.next()
    }
    return solutions
}

fun parseSolutionXml(context: Context, filePath: String): Solution? {
    val inputStream = context.assets.open("collage/$filePath")
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(inputStream, "UTF-8")

    var eventType = parser.eventType
    var currentTag: String?
    var solution: Solution? = null
    val pictures = mutableListOf<Picture>()
    val pictureAreas = mutableListOf<PictureArea>()

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                currentTag = parser.name
                when {
                    currentTag.equals("Solution", true) -> {
                        val id = parser.getAttributeValue(null, "id")
                        val type = parser.getAttributeValue(null, "type")
                        val thumb = parser.getAttributeValue(null, "thumb")
                        val width = parser.getAttributeValue(null, "width").toInt()
                        val height = parser.getAttributeValue(null, "height")?.toIntOrNull()
                        solution = Solution(id, type, thumb, width, height, pictures, pictureAreas)
                    }

                    currentTag.equals("Picture", true) -> {
                        val id = parser.getAttributeValue(null, "id")
                        val type = parser.getAttributeValue(null, "type")
                        val src = parser.getAttributeValue(null, "src")
                        val fillPolygon = parser.getAttributeValue(null, "fillPolygon").toBoolean()
                        val alpha = parser.getAttributeValue(null, "alpha").toInt()
                        val rotate = parser.getAttributeValue(null, "ratote").toInt() // Note the typo in "ratote" if it's in the XML
                        val left = parser.getAttributeValue(null, "left")?.toIntOrNull() ?: 0
                        val top = parser.getAttributeValue(null, "top")?.toIntOrNull() ?: 0
                        val right = parser.getAttributeValue(null, "right")?.toIntOrNull() ?: 0
                        val bottom = parser.getAttributeValue(null, "bottom")?.toIntOrNull() ?: 0
                        val width = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 0
                        val height = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: 0
                        val hint = parser.getAttributeValue(null, "hint")

                        val picture = Picture(id, type, src, fillPolygon, alpha, rotate, left, top, right, bottom, width, height, hint)
                        pictures.add(picture)
                    }

                    currentTag.equals("PictureArea", true) -> {
                        val id = parser.getAttributeValue(null, "id")
                        val type = parser.getAttributeValue(null, "type")
                        val src = parser.getAttributeValue(null, "src")
                        val backgroundColor = parser.getAttributeValue(null, "backgroundColor")
                        val alpha = parser.getAttributeValue(null, "alpha").toInt()
                        val paddingLeft = parser.getAttributeValue(null, "paddingLeft").toInt()
                        val paddingRight = parser.getAttributeValue(null, "paddingRight").toInt()
                        val paddingTop = parser.getAttributeValue(null, "paddingTop").toInt()
                        val paddingBottom = parser.getAttributeValue(null, "paddingBottom").toInt()
                        val spacing = parser.getAttributeValue(null, "spacing").toInt()

                        val pictureArea = PictureArea(id, type, src, backgroundColor, alpha, paddingLeft, paddingRight, paddingTop, paddingBottom, spacing)
                        pictureAreas.add(pictureArea)
                    }
                }
            }
        }
        eventType = parser.next()
    }
    return solution
}