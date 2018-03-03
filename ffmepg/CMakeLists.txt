cmake_minimum_required(VERSION 3.4.1)
add_library( # Sets the name of the library.
             native-lib
             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp)
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )
target_link_libraries( # Specifies the target library.
                       native-lib
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

add_library( avcodec-56
             #STATIC表示.a库，SHARED表示.so库
             SHARED
             #IMPORT表示只需要导入，不需要构建so
             IMPORTED )

set_target_properties(
                      #so库的名称
                      avcodec-56
                      #import so库
                      PROPERTIES IMPORTED_LOCATION
                      #so库路径
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/armeabi/include.libavcodec-56.so
)