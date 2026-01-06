cmake_minimum_required(VERSION 3.31.6)

project(ai_native LANGUAGES C CXX)

# -------------------------------
# Standards
# -------------------------------
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(WHISPER_VERSION "unknown" PARENT_SCOPE)

# -------------------------------
# Android ABI handling
# -------------------------------
if (DEFINED ANDROID_ABI)
    message(STATUS "Android ABI: ${ANDROID_ABI}")

    if (ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH ARM)
        set(GGML_CPU_KLEIDIAI ON)
        set(GGML_OPENMP ON)
        set(GGML_COMPILE_OPTIONS -march=armv8.2-a+fp16)
    elseif (ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH x86)
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP OFF)
        set(GGML_COMPILE_OPTIONS "")
    elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
        set(GGML_SYSTEM_ARCH ARM)
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP OFF)
        set(GGML_COMPILE_OPTIONS -mfpu=neon-vfpv4)
    else()
        message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
    endif()
endif()

# -------------------------------
# Paths
# -------------------------------
set(LLAMA_DIR   ${CMAKE_SOURCE_DIR}/../../../../llama.cpp)
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/../../../../whisper.cpp)
set(GGML_DIR    ${LLAMA_DIR}/ggml)   # shared GGML

# -------------------------------
# GGML
# -------------------------------
add_subdirectory(${GGML_DIR} build-ggml)
target_compile_options(ggml PRIVATE ${GGML_COMPILE_OPTIONS})

# -------------------------------
# llama.cpp
# -------------------------------
add_subdirectory(${LLAMA_DIR} build-llama)

# -------------------------------
# whisper.cpp (build as STATIC, then link into ai_native)
# -------------------------------
set(WHISPER_SRC
        ${WHISPER_DIR}/src/whisper.cpp
)

add_library(whisper STATIC ${WHISPER_SRC})

target_include_directories(whisper PUBLIC
        ${WHISPER_DIR}
        ${WHISPER_DIR}/src
        ${WHISPER_DIR}/include
)

target_compile_definitions(whisper PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
        GGML_USE_CPU
)

target_link_libraries(whisper PRIVATE ggml)

# -------------------------------
# Main shared library
# -------------------------------
add_library(ai_native SHARED
        ai_chat.cpp
        jni.c
)

# Compile definitions
target_compile_definitions(ai_native PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
        GGML_USE_CPU
)

# Include directories
target_include_directories(ai_native PRIVATE
        ${LLAMA_DIR}
        ${LLAMA_DIR}/common
        ${LLAMA_DIR}/include

        ${GGML_DIR}/include
        ${GGML_DIR}/src
)

# Link libraries
target_link_libraries(ai_native
        PRIVATE
        llama
        common
        ggml
        whisper   # <- whisper code now linked statically
        android
        ${LOG_LIB}
)

# -------------------------------
# Release optimizations
# -------------------------------
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(ai_native PRIVATE
            -O3
            -fvisibility=hidden
            -fvisibility-inlines-hidden
            -ffunction-sections
            -fdata-sections
    )

    target_link_options(ai_native PRIVATE
            -Wl,--gc-sections
            -Wl,--exclude-libs,ALL
            -flto
    )
endif()
