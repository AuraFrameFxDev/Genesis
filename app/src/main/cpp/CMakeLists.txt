cmake_minimum_required(VERSION 3.18.1)

# Set project name
project(language_id_l2c_jni LANGUAGES C CXX)

# Set library name
set(LIBRARY_NAME ${PROJECT_NAME})

# Set C++ standard and properties
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set common compile flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -fexceptions -fno-limit-debug-info")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -fexceptions -fno-limit-debug-info")

# Add JNI includes
include_directories(
    ${CMAKE_ANDROID_NDK}/sources/android/native_app_glue
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Find required libraries
find_library(log-lib log)

# Set output directories for different build types
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/${CMAKE_BUILD_TYPE}/${ANDROID_ABI})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/${CMAKE_BUILD_TYPE}/${ANDROID_ABI})

# Create the shared library
add_library(
    ${LIBRARY_NAME}
    SHARED
    language_id_l2c_jni.cpp
)

# Set target properties
target_compile_definitions(${LIBRARY_NAME} PRIVATE
    $<$<CONFIG:Debug>:DEBUG>
    -DANDROID
    -DANDROID_NDK
    $<$<CONFIG:Release>:-DNDEBUG>
)

# Link libraries
target_link_libraries(
    ${LIBRARY_NAME}
    ${log-lib}
    android
)

# Set output name to match Android naming conventions
set_target_properties(${LIBRARY_NAME} PROPERTIES
    OUTPUT_NAME "${LIBRARY_NAME}"
    PREFIX ""
    SUFFIX ".so"
    VERSION 1.0.0
    SOVERSION 1
)

# Set compile options
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(${LIBRARY_NAME} PRIVATE -O0 -g)
else()
    target_compile_options(${LIBRARY_NAME} PRIVATE -O3)
endif()

# Install the library
install(TARGETS ${LIBRARY_NAME}
    LIBRARY DESTINATION lib/${ANDROID_ABI}
    ARCHIVE DESTINATION lib/${ANDROID_ABI}
    RUNTIME DESTINATION bin/${ANDROID_ABI}
)
