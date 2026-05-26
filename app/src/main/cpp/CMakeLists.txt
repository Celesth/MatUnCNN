cmake_minimum_required(VERSION 3.22)
project("matuncnn-native")

set(VULKAN_INCLUDE ${ANDROID_NDK}/sources/third_party/vulkan/include)

# ─── Vulkan GPU info probe ──────────────────────────────────
# Runs before ncnn to detect Adreno via Vulkan API (more
# accurate than Build.SOC_MODEL). Java ExecHelper runs this
# first, parses "gpu:0x5143:..." output, then decides -g -1.
add_executable(matuncnn-vkinfo matuncnn_launcher.cpp)
target_include_directories(matuncnn-vkinfo PRIVATE ${VULKAN_INCLUDE})
target_link_libraries(matuncnn-vkinfo PRIVATE vulkan)

# ─── LD_PRELOAD hook ─────────────────────────────────────────
# Injected into ncnn's process via LD_PRELOAD to intercept
# Vulkan calls. Logs vkQueueSubmit -4, can spoof API version
# to work around bug_buffer_image_load_zero.
add_library(vulkan-hook SHARED vulkan_hook.cpp)
target_include_directories(vulkan-hook PRIVATE ${VULKAN_INCLUDE})
target_link_libraries(vulkan-hook PRIVATE vulkan)
