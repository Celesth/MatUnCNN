cmake_minimum_required(VERSION 3.22)
project("matuncnn-native")

set(VULKAN_INCLUDE ${ANDROID_NDK}/sources/third_party/vulkan/include)

# ─── JNI Vulkan probe library ────────────────────────────────
# Loaded via System.loadLibrary("matuncnn-native") from Java.
# Calls Vulkan API directly to detect GPU vendor — no noexec
# bypass needed since it runs in the app process.
add_library(matuncnn-native SHARED matuncnn_launcher.cpp)
target_include_directories(matuncnn-native PRIVATE ${VULKAN_INCLUDE})
target_link_libraries(matuncnn-native PRIVATE vulkan jnigraphics log)

set_target_properties(matuncnn-native PROPERTIES
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED ON
)

# ─── LD_PRELOAD hook (optional) ──────────────────────────────
# Injected into ncnn's process via LD_PRELOAD to intercept
# Vulkan calls. Set env["MATUNCNN_SPOOF_VK_API"]=1 to skip
# ncnn's bug_buffer_image_load_zero workaround (root cause of
# Adreno black image bug).
add_library(vulkan-hook SHARED vulkan_hook.cpp)
target_include_directories(vulkan-hook PRIVATE ${VULKAN_INCLUDE})
target_link_libraries(vulkan-hook PRIVATE vulkan)

set_target_properties(vulkan-hook PROPERTIES
    CXX_STANDARD 11
    CXX_STANDARD_REQUIRED ON
)
