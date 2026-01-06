cmake_minimum_required(VERSION 3.14)
project(whisper LANGUAGES C CXX)

# Android only
if (NOT ANDROID)
    message(FATAL_ERROR "Whisper is built only as Android shared lib")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# IMPORTANT: build a shared library
add_library(whisper SHARED
        src/whisper.cpp
)

# ggml already exists (from llama)
target_link_libraries(whisper
        ggml
        ggml-base
        ggml-cpu
        log
)

target_include_directories(whisper PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
