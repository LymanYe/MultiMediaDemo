cmake_minimum_required(VERSION 3.4.1)

#C++编译
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

set(jnilibs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${jnilibs}/${ANDROID_ABI})

add_library( # Sets the name of the library.
             basicinfo-lib
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/basicinfo-lib.c)

add_library( # Sets the name of the library.
             streamer-lib
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/streamer-lib.c)

#第三方so存放的根目录
SET(
    third_path
    ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}
    )
#引入源文件.h目录
include_directories( ${CMAKE_SOURCE_DIR}/src/main/cpp/include)


#设置libavcodec.so
add_library(
              libffmpeg
              SHARED
              IMPORTED
)

set_target_properties(libffmpeg
                      PROPERTIES IMPORTED_LOCATION
                      ${third_path}/libffmpeg.so
)


find_library( # Sets the name of the path variable.
               log-lib
               # Specifies the name of the NDK library that
               # you want CMake to locate.
               log )

target_link_libraries(
                       basicinfo-lib
                       libffmpeg
                       ${log-lib})

target_link_libraries(
                       streamer-lib
                       libffmpeg
                       ${log-lib})
