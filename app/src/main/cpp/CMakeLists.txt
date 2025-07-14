cmake_minimum_required(VERSION 3.22.1)

# Set project name and version
project(language_id_l2c_jni
        VERSION 1.0.0
        LANGUAGES C CXX
)

# Set library name
set(LIBRARY_NAME ${PROJECT_NAME})

# Set C++ standard and properties
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set C++ standard and properties
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Set common compile flags for all configurations
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -fexceptions -fvisibility=hidden -fno-limit-debug-info")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -fexceptions -fvisibility=hidden -fvisibility-inlines-hidden -fno-limit-debug-info")

# Configuration-specific flags
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -DDEBUG=1 -g -O0")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DDEBUG=1 -g -O0")
else ()
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O2 -DNDEBUG")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")
endif ()

# Add JNI includes
include_directories(
        ${CMAKE_ANDROID_NDK}/sources/android/native_app_glue
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Find required packages
find_package(Threads REQUIRED)
find_library(log-lib log)

# Set output directories
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/${CMAKE_BUILD_TYPE}/${ANDROID_ABI})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/${CMAKE_BUILD_TYPE}/${ANDROID_ABI})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/intermediates/cxx/${CMAKE_BUILD_TYPE}/${ANDROID_ABI})

# Add source files (exclude test files)
file(GLOB_RECURSE SRC_FILES
        "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/*.c"
)

# Filter out test files from main sources
list(FILTER SRC_FILES EXCLUDE REGEX ".*_test\.cpp$")

# Find test files
file(GLOB_RECURSE TEST_FILES
        "${CMAKE_CURRENT_SOURCE_DIR}/*_test.cpp"
)

# Create the shared library
add_library(
        ${LIBRARY_NAME}
        SHARED
        ${SRC_FILES}
)

# Add test executable if tests are enabled
if (BUILD_TESTING AND TEST_FILES)
    enable_testing()

    # Create test executable
    add_executable(${LIBRARY_NAME}_test
            ${TEST_FILES}
    )

    # Link test executable with main library and gtest
    target_link_libraries(${LIBRARY_NAME}_test PRIVATE
            ${LIBRARY_NAME}
            gtest
            gmock
            ${log-lib}
    )

    # Add test
    add_test(NAME ${LIBRARY_NAME}_test
            COMMAND ${LIBRARY_NAME}_test
    )
endif ()

# Set target properties
target_compile_definitions(${LIBRARY_NAME} PRIVATE
        $<$<CONFIG:Debug>:DEBUG>
        -DANDROID
        -DANDROID_NDK
        -DANDROID_STL="c++_shared"
        $<$<CONFIG:Release>:-DNDEBUG>
)

# Set include directories
target_include_directories(${LIBRARY_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_ANDROID_NDK}/sources/android/native_app_glue
)

# Link libraries
target_link_libraries(
        ${LIBRARY_NAME} PRIVATE
        ${log-lib}
        android
        atomic
)

# Set output name to match Android naming conventions
set_target_properties(${LIBRARY_NAME} PROPERTIES
        OUTPUT_NAME "${LIBRARY_NAME}"
        PREFIX ""
        SUFFIX ".so"
        VERSION ${PROJECT_VERSION}
        SOVERSION 1
        CXX_EXTENSIONS OFF
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
        POSITION_INDEPENDENT_CODE ON
)

# Set compile options based on build type
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(${LIBRARY_NAME} PRIVATE
            -O0
            -g
            -fno-omit-frame-pointer
    )
    target_compile_definitions(${LIBRARY_NAME} PRIVATE
            -DDEBUG=1
            -D_DEBUG=1
    )
else ()
    target_compile_options(${LIBRARY_NAME} PRIVATE
            -O3
            -DNDEBUG
            -fomit-frame-pointer
            -fstrict-aliasing
    )
    target_compile_definitions(${LIBRARY_NAME} PRIVATE
            -DNDEBUG=1
    )
endif ()

# Add install target
install(TARGETS ${LIBRARY_NAME}
        LIBRARY DESTINATION lib/${ANDROID_ABI}
        ARCHIVE DESTINATION lib/${ANDROID_ABI}
        RUNTIME DESTINATION bin/${ANDROID_ABI}
        INCLUDES DESTINATION include
)

# Uninstall target removed as it's not needed for Android builds
# Add code formatting target
find_program(CLANG_FORMAT "clang-format")
if (CLANG_FORMAT)
    add_custom_target(format
            COMMAND ${CLANG_FORMAT}
            -i
            -style=file
            ${SRC_FILES}
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )
endif ()
