cmake_minimum_required(VERSION 3.14)
project("whisper.cpp" C CXX)

# Android only
if (NOT ANDROID)
    message(FATAL_ERROR "Whisper is built only as Android shared lib")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build shared library
add_library(whisper SHARED
        src/whisper.cpp
)

# Required when linking into another .so
set_target_properties(whisper PROPERTIES
        POSITION_INDEPENDENT_CODE ON
)

# ggml already provided by llama
target_link_libraries(whisper
        ggml
        ggml-base
        ggml-cpu
        log
)

# Headers must be visible to consumers
target_include_directories(whisper PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)
