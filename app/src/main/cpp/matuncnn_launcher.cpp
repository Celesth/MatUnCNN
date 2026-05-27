// matuncnn-native: JNI bridge for Vulkan GPU probe.
// Called from VulkanHelper.kt to detect Adreno via Vulkan API
// (more reliable than Build.SOC_MODEL).

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>
#include <jni.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
#include <string>

// ---------------------------------------------------------------------------
// Dynamic Vulkan loader
// ---------------------------------------------------------------------------

struct VkLoader {
    void* lib = nullptr;
    PFN_vkCreateInstance pCreateInstance = nullptr;
    PFN_vkDestroyInstance pDestroyInstance = nullptr;
    PFN_vkEnumeratePhysicalDevices pEnumeratePhysicalDevices = nullptr;
    PFN_vkGetPhysicalDeviceProperties pGetPhysicalDeviceProperties = nullptr;

    bool load() {
        lib = dlopen("libvulkan.so", RTLD_LAZY | RTLD_LOCAL);
        if (!lib) lib = dlopen("libvulkan.so.1", RTLD_LAZY | RTLD_LOCAL);
        if (!lib) return false;
        pCreateInstance = (PFN_vkCreateInstance)dlsym(lib, "vkCreateInstance");
        pDestroyInstance = (PFN_vkDestroyInstance)dlsym(lib, "vkDestroyInstance");
        pEnumeratePhysicalDevices = (PFN_vkEnumeratePhysicalDevices)dlsym(lib, "vkEnumeratePhysicalDevices");
        pGetPhysicalDeviceProperties = (PFN_vkGetPhysicalDeviceProperties)dlsym(lib, "vkGetPhysicalDeviceProperties");
        if (!pCreateInstance || !pDestroyInstance || !pEnumeratePhysicalDevices || !pGetPhysicalDeviceProperties) {
            dlclose(lib);
            lib = nullptr;
            return false;
        }
        return true;
    }
    void unload() { if (lib) { dlclose(lib); lib = nullptr; } }
};

// ---------------------------------------------------------------------------
// JNI: probe Vulkan GPU vendor
// Returns: JSON array of GPU info, or "[]" on error
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jstring JNICALL
Java_com_matuncnn_app_util_VulkanHelper_nativeProbeVulkan(
    JNIEnv* env, jclass /*clazz*/)
{
    VkLoader vl;
    if (!vl.load()) {
        return env->NewStringUTF("[]");
    }

    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "matuncnn";
    appInfo.apiVersion = VK_MAKE_VERSION(1, 1, 0);

    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    if (vl.pCreateInstance(&ci, nullptr, &instance) != VK_SUCCESS) {
        vl.unload();
        return env->NewStringUTF("[]");
    }

    uint32_t count = 0;
    vl.pEnumeratePhysicalDevices(instance, &count, nullptr);
    if (count == 0) {
        vl.pDestroyInstance(instance, nullptr);
        vl.unload();
        return env->NewStringUTF("[]");
    }

    // Build JSON array
    std::string json = "[";
    VkPhysicalDevice devices[8];
    if (count > 8) count = 8;
    vl.pEnumeratePhysicalDevices(instance, &count, devices);

    for (uint32_t i = 0; i < count; i++) {
        VkPhysicalDeviceProperties props{};
        vl.pGetPhysicalDeviceProperties(devices[i], &props);
        if (i > 0) json += ",";
        json += "{\"vendorID\":" + std::to_string(props.vendorID);
        json += ",\"deviceID\":" + std::to_string(props.deviceID);
        json += ",\"apiVersion\":" + std::to_string(props.apiVersion);
        json += ",\"name\":\"" + std::string(props.deviceName) + "\"}";
    }
    json += "]";

    vl.pDestroyInstance(instance, nullptr);
    vl.unload();

    return env->NewStringUTF(json.c_str());
}
