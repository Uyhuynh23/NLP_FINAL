cmake_minimum_required(VERSION 3.22.1)

project("espeak_phonemizer")

# Create the native library
add_library(espeak_phonemizer SHARED
    espeak_jni.cpp
)

# Find and link log library
find_library(log-lib log)

target_link_libraries(espeak_phonemizer
    ${log-lib}
)

