# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Declares and names the project.

project("libnative")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        libnative

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        libnative.cpp
        LibAlpha.cpp
        photo_processing.c
        )

set(OPENCV_SDK_PATH  ${CMAKE_SOURCE_DIR}/opencv_native)#设置OpenCv的路径变量

include_directories(${OPENCV_SDK_PATH}/jni/include)

set(OPENCV_STATIC_LIBS "${OPENCV_SDK_PATH}/staticlibs")

set(OPENCV_ANDROID_ABI "${ANDROID_ABI}")
link_directories("${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library(libcpufeatures STATIC IMPORTED )
set_target_properties(libcpufeatures PROPERTIES IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libcpufeatures.a")

add_library(tbb STATIC IMPORTED )
set_target_properties(tbb PROPERTIES
        INTERFACE_COMPILE_DEFINITIONS "TBB_USE_GCC_BUILTINS=1;__TBB_GCC_BUILTIN_ATOMICS_PRESENT=1;TBB_SUPPRESS_DEPRECATED_MESSAGES=1"
        INTERFACE_LINK_LIBRARIES "c;m;dl" IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libtbb.a")

if ("${OPENCV_ANDROID_ABI}" MATCHES ".*x86.*")
    add_library(ippiw STATIC IMPORTED)
    set_target_properties(ippiw PROPERTIES IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libippiw.a")
    add_library(ippicv STATIC IMPORTED)
    set_target_properties(ippicv PROPERTIES IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libippicv.a")
else()
    add_library(tegra_hal STATIC IMPORTED )
    set_target_properties(tegra_hal PROPERTIES IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libtegra_hal.a")
endif()

add_library(ittnotify STATIC IMPORTED )
set_target_properties(ittnotify PROPERTIES INTERFACE_LINK_LIBRARIES "dl" IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libittnotify.a")


add_library(opencv_core STATIC IMPORTED )
if ("${OPENCV_ANDROID_ABI}" MATCHES ".*x86.*")
    set_target_properties(opencv_core PROPERTIES
            INTERFACE_LINK_LIBRARIES "\$<LINK_ONLY:dl>;\$<LINK_ONLY:m>;\$<LINK_ONLY:log>;\$<LINK_ONLY:ippiw>;\$<LINK_ONLY:ippicv>;\$<LINK_ONLY:tbb>;\$<LINK_ONLY:z>;\$<LINK_ONLY:libcpufeatures>;\$<LINK_ONLY:ittnotify>"
            IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libopencv_core.a")
else()
    set_target_properties(opencv_core PROPERTIES
            INTERFACE_LINK_LIBRARIES "\$<LINK_ONLY:dl>;\$<LINK_ONLY:m>;\$<LINK_ONLY:log>;\$<LINK_ONLY:tegra_hal>;\$<LINK_ONLY:tbb>;\$<LINK_ONLY:z>;\$<LINK_ONLY:libcpufeatures>;\$<LINK_ONLY:ittnotify>;\$<LINK_ONLY:tegra_hal>"
            IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libopencv_core.a")
endif()

add_library(opencv_imgproc STATIC IMPORTED)
if ("${OPENCV_ANDROID_ABI}" MATCHES ".*x86.*")
    set_target_properties(opencv_imgproc PROPERTIES
            INTERFACE_LINK_LIBRARIES "opencv_core;opencv_core;\$<LINK_ONLY:dl>;\$<LINK_ONLY:m>;\$<LINK_ONLY:log>;\$<LINK_ONLY:ippiw>;\$<LINK_ONLY:ippicv>"
            IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libopencv_imgproc.a")
else()
    set_target_properties(opencv_imgproc PROPERTIES
            INTERFACE_LINK_LIBRARIES "opencv_core;opencv_core;\$<LINK_ONLY:dl>;\$<LINK_ONLY:m>;\$<LINK_ONLY:log>;\$<LINK_ONLY:tegra_hal>"
            IMPORTED_LOCATION "${OPENCV_STATIC_LIBS}/${OPENCV_ANDROID_ABI}/libopencv_imgproc.a")
endif()

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
if ("${OPENCV_ANDROID_ABI}" MATCHES ".*x86.*")
    target_link_libraries( # Specifies the target library.
            libnative
            libcpufeatures
            tbb
            ippiw
            ippicv
            ittnotify
            opencv_core
            opencv_imgproc
            -ljnigraphics
            GLESv2

            # Links the target library to the log library
            # included in the NDK.
            ${log-lib}
            )
else()
    target_link_libraries( # Specifies the target library.
            libnative
            libcpufeatures
            tbb
            tegra_hal
            ittnotify
            opencv_core
            opencv_imgproc
            -ljnigraphics
            GLESv2

            # Links the target library to the log library
            # included in the NDK.
            ${log-lib}
            )
endif()