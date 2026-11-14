cmake_minimum_required(VERSION 3.22.1)
project(cartdeck_wiiu_vulkan_host LANGUAGES CXX)
if(NOT ANDROID)
    message(FATAL_ERROR "Wii U host is Android ARM64 only")
endif()
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "Wii U host requires Android arm64-v8a")
endif()
if(NOT DEFINED CEMU_LIBRETRO_HEADERS)
    message(FATAL_ERROR "Set CEMU_LIBRETRO_HEADERS to the verified pinned Cemu source headers")
endif()
if(NOT EXISTS "${CEMU_LIBRETRO_HEADERS}/libretro.h" OR
   NOT EXISTS "${CEMU_LIBRETRO_HEADERS}/libretro_vulkan.h")
    message(FATAL_ERROR "Pinned Cemu Libretro/Vulkan headers are missing")
endif()

add_library(cartdeckwiiuvulkanhost SHARED
    CartdeckVulkanPresenter.cpp
    WiiuLibretroRunner.cpp
)
target_include_directories(cartdeckwiiuvulkanhost PRIVATE "${CEMU_LIBRETRO_HEADERS}")
target_compile_features(cartdeckwiiuvulkanhost PRIVATE cxx_std_17)
target_compile_options(cartdeckwiiuvulkanhost PRIVATE -Wall -Wextra)
target_link_options(cartdeckwiiuvulkanhost PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries(cartdeckwiiuvulkanhost PRIVATE android vulkan log dl)
