cmake_minimum_required(VERSION 3.4.1)

set(
	PATH_TO_SUPERPOWERED
	CACHE STRING ""
)

message(${ANDROID_ABI})

file(GLOB CPP_FILES "*.cpp")

add_library(
	FilterProcess
	SHARED
	${CPP_FILES}
)

include_directories(src/main/jni)
include_directories(${PATH_TO_SUPERPOWERED})

target_link_libraries(
    FilterProcess
    log
    android
    OpenSLES
    /Users/ferko/SuperpoweredSDK/Superpowered/libSuperpoweredAndroid${ANDROID_ABI}.a
)