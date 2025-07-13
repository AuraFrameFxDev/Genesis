# app/src/main/cpp/CMakeLists.txt

# Set shorter paths for build outputs
set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/out)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/out)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/out)

# Set minimum required CMake version
cmake_minimum_required(VERSION 3.22.1)

# Configure project with shorter name
project("aura" LANGUAGES CXX)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add library with shorter name
add_library(aura-lib SHARED native-lib.cpp)

# Set output name to match the original
set_target_properties(aura-lib PROPERTIES OUTPUT_NAME "aura-native-lib")

# Find and link required libraries
find_library(log-lib log)
target_link_libraries(aura-lib PRIVATE ${log-lib})

# Set compile options
target_compile_options(aura-lib PRIVATE
        -Wall
        -Werror
        -fexceptions
        -frtti
)

# Set include directories
target_include_directories(aura-lib PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
)