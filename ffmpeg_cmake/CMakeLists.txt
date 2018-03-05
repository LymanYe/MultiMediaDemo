cmake_minimum_required(VERSION 3.4.1)

set(jnilibs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${jnilibs}/${ANDROID_ABI})


add_library( # Sets the name of the library.
             native-lib
             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.c)
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )



#第三方so存放的根目录
SET(
    third_path
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}
    )
#引入源文件.h目录
include_directories( ${CMAKE_SOURCE_DIR}/src/main/cpp/include)


#设置libavcodec.so
add_library(libavcodec-57
              SHARED
              IMPORTED
)

set_target_properties(libavcodec-57
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libavcodec-57.so
)
#设置libavdevice.so
add_library(libavdevice-57
              SHARED
              IMPORTED
)

set_target_properties(libavdevice-57
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libavdevice-57.so
)

#设置libavfilter.so
 add_library(libavfilter-6
               SHARED
               IMPORTED
 )

 set_target_properties(libavfilter-6
                       PROPERTIES IMPORTED_LOCATION
                       ${third_path}/libavfilter-6.so
 )

#设置libavformat.so
add_library(libavformat-57
              SHARED
              IMPORTED
)

set_target_properties(libavformat-57
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libavformat-57.so
)

#设置libavutil.so
add_library(libavutil-55
              SHARED
              IMPORTED
)

set_target_properties(libavutil-55
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libavutil-55.so
)

#设置libpostproc.so
add_library(libpostproc-54
              SHARED
              IMPORTED
)

set_target_properties(libpostproc-54
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libpostproc-54.so
)

#设置libswresample.so
 add_library(libswresample-2
               SHARED
               IMPORTED
 )

 set_target_properties(libswresample-2
                       PROPERTIES IMPORTED_LOCATION
                       ${third_path}/libswresample-2.so
 )

 #设置libswscale.so
 add_library(libswscale-4
               SHARED
               IMPORTED
 )

 set_target_properties(libswscale-4
                       PROPERTIES IMPORTED_LOCATION
                       ${third_path}/libswscale-4.so
 )

target_link_libraries(
                       native-lib
                       libavcodec-57
                       libavdevice-57
                       libavfilter-6
                       libavformat-57
                       libavutil-55
                       libpostproc-54
                       libswresample-2
                       libswscale-4
                       ${log-lib} )