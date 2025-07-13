cmake_minimum_required(VERSION 3.18.1)

# Set project name
project(language_id_l2c_jni LANGUAGES C CXX)


# Set library name
set(LIBRARY_NAME ${PROJECT_NAME})


# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add JNI includes
include_directories(${CMAKE_ANDROID_NDK}/sources/android/native_app_glue)

# Find log library
find_library(log-lib log)

# Create the shared library
add_library(
        ${LIBRARY_NAME}
        SHARED
        language_id_l2c_jni.cpp
)

# Link libraries
target_link_libraries(
        ${LIBRARY_NAME}
        ${log-lib}
        android
)

# Set compile definitions
target_compile_definitions(${LIBRARY_NAME} PRIVATE
        $<$<CONFIG:Debug>:DEBUG>
        -DANDROID
)

# Set output directories
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/Debug/${ANDROID_ABI})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/Debug/${ANDROID_ABI})

# Set output name to match Android naming conventions
set_target_properties(${LIBRARY_NAME} PROPERTIES
        OUTPUT_NAME "${LIBRARY_NAME}"
        PREFIX ""
        SUFFIX ".so"
)
