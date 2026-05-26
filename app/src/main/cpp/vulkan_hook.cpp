// vulkan-hook: LD_PRELOAD library that intercepts Vulkan calls
// made by ncnn binaries. Used to trace/log GPU driver issues
// on Adreno (black image, vkQueueSubmit -4).
//
// Usage (set in Java before exec):
//   env["LD_PRELOAD"] = workDir + "/libvulkan-hook.so"
//
// Or copy to a system path that the linker already searches.
//
// Hooked functions:
//   vkQueueSubmit     - logs the -4 error and stack trace
//   vkCreateComputePipelines - logs pipeline creation failures
//   vkGetPhysicalDeviceProperties - can spoof API version to
//                                   work around bug_buffer_image_load_zero

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
#include <unistd.h>
#include <sys/syscall.h>
#include <pthread.h>

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

static FILE* log_fp = nullptr;

__attribute__((constructor))
static void hook_init() {
    // Append to the app's debug log file; falls back to stderr
    const char* path = getenv("MATUNCNN_HOOK_LOG");
    if (path && path[0]) {
        log_fp = fopen(path, "a");
    }
    if (!log_fp) log_fp = stderr;
    fprintf(log_fp, "[vulkan-hook] loaded (pid=%d, tid=%ld)\n",
            getpid(), (long)syscall(SYS_gettid));
    fflush(log_fp);
}

__attribute__((destructor))
static void hook_fini() {
    if (log_fp && log_fp != stderr) fclose(log_fp);
}

static void log_vk_result(const char* func, VkResult res,
                          const char* detail = nullptr) {
    if (res == VK_SUCCESS) return; // don't log successes
    fprintf(log_fp, "[vulkan-hook] %s returned %d",
            func, (int)res);
    if (detail) fprintf(log_fp, " (%s)", detail);
    fprintf(log_fp, " [pid=%d]\n", getpid());
    fflush(log_fp);
}

// ---------------------------------------------------------------------------
// Real function pointers (loaded from libvulkan.so)
// ---------------------------------------------------------------------------

static struct {
    PFN_vkQueueSubmit Real_vkQueueSubmit = nullptr;
    PFN_vkCreateComputePipelines Real_vkCreateComputePipelines = nullptr;
    PFN_vkGetPhysicalDeviceProperties Real_vkGetPhysicalDeviceProperties = nullptr;
    PFN_vkCreateDevice Real_vkCreateDevice = nullptr;
    PFN_vkDestroyDevice Real_vkDestroyDevice = nullptr;
    PFN_vkDeviceWaitIdle Real_vkDeviceWaitIdle = nullptr;
} real;

// ---------------------------------------------------------------------------
// Hook: vkQueueSubmit
// Logs the failure and captures context.
// ---------------------------------------------------------------------------

extern "C" VkResult vkQueueSubmit(
    VkQueue queue, uint32_t submitCount,
    const VkSubmitInfo* pSubmits, VkFence fence)
{
    if (!real.Real_vkQueueSubmit) {
        real.Real_vkQueueSubmit =
            (PFN_vkQueueSubmit)dlsym(RTLD_NEXT, "vkQueueSubmit");
    }

    VkResult res = real.Real_vkQueueSubmit(queue, submitCount, pSubmits, fence);
    if (res < 0) {  // VK_ERROR_* are negative
        fprintf(log_fp, "[vulkan-hook] vkQueueSubmit failed: %d "
                "(submitCount=%u, fence=0x%llx)\n",
                (int)res, submitCount, (unsigned long long)(uintptr_t)fence);
        fflush(log_fp);
    }
    return res;
}

// ---------------------------------------------------------------------------
// Hook: vkCreateComputePipelines
// Logs failures during shader compilation.
// ---------------------------------------------------------------------------

extern "C" VkResult vkCreateComputePipelines(
    VkDevice device, VkPipelineCache pipelineCache,
    uint32_t createInfoCount,
    const VkComputePipelineCreateInfo* pCreateInfos,
    const VkAllocationCallbacks* pAllocator,
    VkPipeline* pPipelines)
{
    if (!real.Real_vkCreateComputePipelines) {
        real.Real_vkCreateComputePipelines =
            (PFN_vkCreateComputePipelines)dlsym(RTLD_NEXT, "vkCreateComputePipelines");
    }

    VkResult res = real.Real_vkCreateComputePipelines(
        device, pipelineCache, createInfoCount,
        pCreateInfos, pAllocator, pPipelines);

    if (res < 0) {
        fprintf(log_fp, "[vulkan-hook] vkCreateComputePipelines failed: %d "
                "(count=%u)\n", (int)res, createInfoCount);
        fflush(log_fp);
    }
    return res;
}

// ---------------------------------------------------------------------------
// Hook: vkGetPhysicalDeviceProperties
// Can spoof apiVersion to work around ncnn's bug_buffer_image_load_zero.
// Enable by setting env MATUNCNN_SPOOF_VK_API=1.
// ncnn enables bug_buffer_image_load_zero when:
//   vendorID == 0x5143 && apiVersion < VK_MAKE_VERSION(1,1,87)
// Spoofing apiVersion to 1.1.87+ skips this workaround, which
// is the actual cause of black images on newer Adreno.
// ---------------------------------------------------------------------------

extern "C" void vkGetPhysicalDeviceProperties(
    VkPhysicalDevice physicalDevice,
    VkPhysicalDeviceProperties* pProperties)
{
    if (!real.Real_vkGetPhysicalDeviceProperties) {
        real.Real_vkGetPhysicalDeviceProperties =
            (PFN_vkGetPhysicalDeviceProperties)dlsym(RTLD_NEXT,
                "vkGetPhysicalDeviceProperties");
    }

    real.Real_vkGetPhysicalDeviceProperties(physicalDevice, pProperties);

    // Optionally spoof API version to skip bug_buffer_image_load_zero
    static const char* spoof = getenv("MATUNCNN_SPOOF_VK_API");
    if (spoof && spoof[0] == '1' &&
        pProperties->vendorID == 0x5143 &&
        pProperties->apiVersion < VK_MAKE_VERSION(1, 1, 87))
    {
        fprintf(log_fp, "[vulkan-hook] Spoofing apiVersion for Adreno "
                "0x%x -> 0x%x (was 0x%x)\n",
                pProperties->deviceID,
                VK_MAKE_VERSION(1, 1, 87),
                pProperties->apiVersion);
        pProperties->apiVersion = VK_MAKE_VERSION(1, 1, 87);
    }
}
