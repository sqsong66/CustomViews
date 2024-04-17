package com.sqsong.opengl.processor.utils

// 方形马赛克
fun getRectMosaicFragmentShader(): String {
    return """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D u_Texture;
    
        uniform float u_AspectRatio; // 纹理的宽高比，宽度除以高度
        uniform float u_MosaicSize; // 马赛克的大小
        void main() {
    
            if (u_MosaicSize <= 0.0) {
                gl_FragColor = texture2D(u_Texture, vTexCoord);
                return;
            }
    
            // 根据宽高比调整纹理坐标，以保证马赛克块的正方形显示
            vec2 adjustedCoord = vec2(vTexCoord.x * u_AspectRatio, vTexCoord.y);
    
            // 计算当前片段在马赛克块中的位置
            vec2 mosaicCoord = vec2(floor(adjustedCoord.x / u_MosaicSize) * u_MosaicSize,
                                    floor(adjustedCoord.y / u_MosaicSize) * u_MosaicSize);
    
            // 获取马赛克块的中心点坐标，用于取样
            vec2 centerCoord = mosaicCoord + vec2(u_MosaicSize / 2.0, u_MosaicSize / 2.0);
    
            // 根据宽高比重新调整中心点坐标，以获取正确的纹理采样点
            vec2 sampleCoord = vec2(centerCoord.x / u_AspectRatio, centerCoord.y);
    
            // 根据中心点坐标取样纹理，得到颜色
            vec4 color = texture2D(u_Texture, sampleCoord);
    
            // 输出颜色
            gl_FragColor = color;
        }
    """.trimIndent()
}

fun getTriangleMosaicFragmentShader(): String {
    return """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D u_Texture;
        uniform float u_MosaicSize; // 三角形的高度

        void main (void) {
            if (u_MosaicSize <= 0.0) {
                gl_FragColor = texture2D(u_Texture, vTexCoord);
                return;
            }

            float length = u_MosaicSize;
            //根据3：√3设定宽高
            //定义矩形的宽
            float TB = 1.5;
            //定义矩形的高
            float TR = 0.866025;
            //π/6的值
            const float PI6 = 0.523599;
            //拿到当前纹理坐标
            float x = vTexCoord.x;
            float y = vTexCoord.y;

            //换算成在矩形中的坐标
            int wx = int(x / TB / length);
            int wy = int(y / TR / length);

            //定义中心点v1  v2 和最终选的的点
            vec2 v1, v2, vn;

            //分4种情况判断取这个点的颜色值。就要先拿到对应2个中心点坐标
            //wx/2 * 2 == wx 就是偶数行   wy/2 * 2 == wy就是偶数列
            if (wx/2 * 2 == wx) {

                if (wy/2 * 2 == wy) {
                    //(0,0),(1,1)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy + 1));
                } else {
                    //(0,1),(1,0)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy + 1));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy));
                }
            }else {
                if (wy/2 * 2 == wy) {
                    //(0,1),(1,0)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy + 1));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy));
                } else {
                    //(0,0),(1,1)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy + 1));
                }
            }


            //计算现在这个像素点到两个中心点之间的距离
            float s1 = sqrt(pow(v1.x - x, 2.0) + pow(v1.y - y, 2.0));
            float s2 = sqrt(pow(v2.x - x, 2.0) + pow(v2.y - y, 2.0));

            //比较，哪个距离小，这个像素点的颜色就取哪个中心点颜色
            if (s1 < s2) {
                vn = v1;
            } else {
                vn = v2;
            }

            //计算像素和中心点vn的夹角
            float a = atan((x - vn.x)/(y - vn.y));

            //计算6个正三角形的中心点
            vec2 area1 = vec2(vn.x, vn.y - u_MosaicSize * TR / 2.0);
            vec2 area2 = vec2(vn.x + u_MosaicSize / 2.0, vn.y - u_MosaicSize * TR / 2.0);
            vec2 area3 = vec2(vn.x + u_MosaicSize / 2.0, vn.y + u_MosaicSize * TR / 2.0);
            vec2 area4 = vec2(vn.x, vn.y + u_MosaicSize * TR / 2.0);
            vec2 area5 = vec2(vn.x - u_MosaicSize / 2.0, vn.y + u_MosaicSize * TR / 2.0);
            vec2 area6 = vec2(vn.x - u_MosaicSize / 2.0, vn.y - u_MosaicSize * TR / 2.0);

            //判断在哪个区域
            if (a >= PI6 && a < PI6 * 3.0) {
                vn = area1;
            } else if (a >= PI6 * 3.0 && a < PI6 * 5.0) {
                vn = area2;
            } else if ((a >= PI6 * 5.0 && a <= PI6 * 6.0)|| (a<-PI6 * 5.0 && a>-PI6*6.0)) {
                vn = area3;
            } else if (a < -PI6 * 3.0 && a >= -PI6 * 5.0) {
                vn = area4;
            } else if(a <= -PI6 && a> -PI6 * 3.0) {
                vn = area5;
            } else if (a > -PI6 && a < PI6)
            {
                vn = area6;
            }

            //拿的最终所在区域的三角形中心点颜色
            vec4 color = texture2D(u_Texture, vn);
            gl_FragColor = color;
        }
    """.trimIndent()
}

fun getHexagonMosaicFragmentShader(): String {
    return """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D u_Texture;
        uniform float u_MosaicSize; // 三角形的高度

        void main() {
            float length = u_MosaicSize;
            //根据3：√3设定宽高
            //定义矩形的宽
            float TB = 1.5;
            //定义矩形的高
            float TR = 0.866025;

            //拿到当前纹理坐标
            float x = vTexCoord.x;
            float y = vTexCoord.y;

            //换算成在矩形中的坐标
            int wx = int(x / TB / length);
            int wy = int(y / TR / length);

            //定义中心点v1  v2 和最终选的的点
            vec2 v1, v2, vn;

            //分4种情况判断取这个点的颜色值。就要先拿到对应2个中心点坐标
            //wx/2 * 2 == wx 就是偶数行   wy/2 * 2 == wy就是偶数列
            if (wx/2 * 2 == wx) {

                if (wy/2 * 2 == wy) {
                    //(0,0),(1,1)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy + 1));
                } else {
                    //(0,1),(1,0)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy + 1));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy));
                }
            }else {
                if (wy/2 * 2 == wy) {
                    //(0,1),(1,0)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy + 1));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy));
                } else {
                    //(0,0),(1,1)
                    v1 = vec2(length * 1.5 * float(wx), length * TR * float(wy));
                    v2 = vec2(length * 1.5 * float(wx + 1), length * TR * float(wy + 1));
                }
            }


            //计算现在这个像素点到两个中心点之间的距离
            float s1 = sqrt(pow(v1.x - x, 2.0) + pow(v1.y - y, 2.0));
            float s2 = sqrt(pow(v2.x - x, 2.0) + pow(v2.y - y, 2.0));

            //比较，哪个距离小，这个像素点的颜色就取哪个中心点颜色
            if (s1 < s2) {
                vn = v1;
            } else {
                vn = v2;
            }
            vec4 color = texture2D(u_Texture, vn);

            gl_FragColor = color;
        }
    """.trimIndent()
}

fun getLutFilterFragmentShader(): String {
    return """
        varying highp vec2 vTexCoord;

        uniform sampler2D u_Texture;
        uniform sampler2D lookupTexture; // lookup texture

        uniform highp float lookupDimension; // 64
        uniform lowp float intensity;

        void main()
        {
            highp vec4 textureColor = texture2D(u_Texture, vTexCoord);
        	
        	//B通道上对应的数值
            highp float blueColor = textureColor.b * (lookupDimension - 1.0);
            highp float lookupDimensionSqrt = sqrt(lookupDimension);

           // 计算临近两个B通道所在的方形LUT单元格（从左到右从，上到下排列）
            highp vec2 quad1;
            quad1.y = floor(floor(blueColor) / lookupDimensionSqrt);
            quad1.x = floor(blueColor) - (quad1.y * lookupDimensionSqrt);

            highp vec2 quad2;
            quad2.y = floor(ceil(blueColor) / lookupDimensionSqrt);
            quad2.x = ceil(blueColor) - (quad2.y * lookupDimensionSqrt);

        	//在对应的小正方形中查找原始图像当前像素锁对应的查找表中的位置
        	//0.5是小正方形里的小正方形的位置取均值
            highp vec2 texPos1;
            texPos1.x = (quad1.x / lookupDimensionSqrt) + 0.5/lookupDimensionSqrt/lookupDimension + ((1.0/lookupDimensionSqrt - 1.0/lookupDimensionSqrt/lookupDimension) * textureColor.r);
            texPos1.y = (quad1.y / lookupDimensionSqrt) + 0.5/lookupDimensionSqrt/lookupDimension + ((1.0/lookupDimensionSqrt - 1.0/lookupDimensionSqrt/lookupDimension) * textureColor.g);

            highp vec2 texPos2;
            texPos2.x = (quad2.x / lookupDimensionSqrt) + 0.5/lookupDimensionSqrt/lookupDimension + ((1.0/lookupDimensionSqrt - 1.0/lookupDimensionSqrt/lookupDimension) * textureColor.r);
            texPos2.y = (quad2.y / lookupDimensionSqrt) + 0.5/lookupDimensionSqrt/lookupDimension + ((1.0/lookupDimensionSqrt - 1.0/lookupDimensionSqrt/lookupDimension) * textureColor.g);

        	//根据当前像素查找到的相邻的两个小正方形的位置从纹理中取出形影的像素颜色值
            lowp vec4 newColor1 = texture2D(lookupTexture, texPos1);
            lowp vec4 newColor2 = texture2D(lookupTexture, texPos2);

        	//根据蓝色通道的值对生成的相邻的两个新图像色值做加权
            lowp vec4 newColor = vec4(mix(newColor1.rgb, newColor2.rgb, fract(blueColor)), textureColor.w);
            //根据intensity控制效果程度
            gl_FragColor = mix(textureColor, newColor, intensity);
        }
    """.trimIndent()
}

fun getLutFilterFragmentShader1(): String {
    return """
        // 目标纹理坐标
        varying highp vec2 vTexCoord;
        // 目标纹理
        uniform sampler2D u_Texture;
        // 查找表纹理
        uniform sampler2D lookupTexture;
        // 目标纹理与查找表纹理混合比例
        uniform lowp float intensity;

        void main()
        {
            //获取原始图层颜色
            highp vec4 textureColor = texture2D(u_Texture, vTexCoord);
            
            //获取蓝色通道颜色，textureColor.b 的范围为(0,1)，blueColor 范围为(0,63) 
            highp float blueColor = textureColor.b * 63.0;
            
            //quad1为查找颜色所在左边位置的小正方形
            highp vec2 quad1;
            quad1.y = floor(floor(blueColor) / 8.0);
            quad1.x = floor(blueColor) - (quad1.y * 8.0);
            
            //quad2为查找颜色所在右边位置的小正方形
            highp vec2 quad2;
            quad2.y = floor(ceil(blueColor) / 8.0);
            quad2.x = ceil(blueColor) - (quad2.y * 8.0);
            
            //获取到左边小方形里面的颜色值
            highp vec2 texPos1;
            texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
            texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
            
            //获取到右边小方形里面的颜色值
            highp vec2 texPos2;
            texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);
            texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);
            
            //获取对应位置纹理的颜色 RGBA 值
            lowp vec4 newColor1 = texture2D(lookupTexture, texPos1);
            lowp vec4 newColor2 = texture2D(lookupTexture, texPos2);
            
            //真正的颜色是 newColor1 和 newColor2 的混合
            lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
            gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);
        }
    """.trimIndent()
}