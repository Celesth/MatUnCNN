// matuncnn-vkinfo: Vulkan GPU info probe for MatUnCNN.
//
// Queries Vulkan physical devices and prints machine-readable
// GPU info to stdout. Called by the Java ExecHelper before
// launching the ncnn binary.
//
// Output format (one line per device):
//   gpu:<vendorID>:<deviceID>:<deviceName>
//
// Example:
//   gpu:0x5143:0x06050002:Adreno (TM) 650
//
// Exit code:
//   0   Vulkan available, info printed
//   1   Vulkan not available / error
//   2   Vulkan loaded but no devices

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>

// ---------------------------------------------------------------------------
// Minimal dynamic Vulkan loader (no C++ stdlib deps)
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

int main() {
    VkLoader vl;
    if (!vl.load()) {
        fprintf(stderr, "[vkinfo] libvulkan.so not found\n");
        return 1;
    }

    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "matuncnn-vkinfo";
    appInfo.apiVersion = VK_MAKE_VERSION(1, 1, 0);

    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    if (vl.pCreateInstance(&ci, nullptr, &instance) != VK_SUCCESS) {
        fprintf(stderr, "[vkinfo] vkCreateInstance failed\n");
        vl.unload();
        return 1;
    }

    uint32_t count = 0;
    vl.pEnumeratePhysicalDevices(instance, &count, nullptr);
    if (count == 0) {
        fprintf(stderr, "[vkinfo] No Vulkan physical devices\n");
        vl.pDestroyInstance(instance, nullptr);
        vl.unload();
        return 2;
    }

    VkPhysicalDevice devices[8];
    if (count > 8) count = 8;
    vl.pEnumeratePhysicalDevices(instance, &count, devices);

    for (uint32_t i = 0; i < count; i++) {
        VkPhysicalDeviceProperties props{};
        vl.pGetPhysicalDeviceProperties(devices[i], &props);
        // Machine-readable: java parses "gpu:" lines
        printf("gpu:0x%x:0x%x:%s\n",
               props.vendorID, props.deviceID, props.deviceName);
    }

    vl.pDestroyInstance(instance, nullptr);
    vl.unload();
    return 0;
}
