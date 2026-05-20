package com.matuncnn.app.util

import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.io.RandomAccessFile

object ExecHelper {
    private const val TAG = "ExecHelper"
    private var method: ExecMethod? = null

    private enum class ExecMethod {
        DIRECT, PROC_SELF_FD, LINKER64
    }

    fun exec(
        binary: File,
        args: List<String>,
        workDir: String,
        extraEnv: Map<String, String> = emptyMap()
    ): Process {
        if (method == null) {
            method = probeMethod(binary, workDir)
        }
        DebugLog.log("Exec", "${method} ${binary.name} ${args.joinToString(" ")}")
        return when (method) {
            ExecMethod.DIRECT -> execDirect(binary, args, workDir, extraEnv)
            ExecMethod.PROC_SELF_FD -> execProcSelfFd(binary, args, workDir, extraEnv)
            ExecMethod.LINKER64 -> execLinker64(binary, args, workDir, extraEnv)
            null -> throw RuntimeException("All execution methods failed")
        }
    }

    private fun probeMethod(binary: File, workDir: String): ExecMethod {
        if (tryMethod { execDirect(binary, emptyList(), workDir).apply { destroy(); waitFor() } }) {
            DebugLog.log("Exec", "Probe: DIRECT works for ${binary.name}")
            return ExecMethod.DIRECT
        }
        DebugLog.log("Exec", "Probe: DIRECT failed for ${binary.name}, trying /proc/self/fd/...")
        Log.i(TAG, "Direct exec failed, trying /proc/self/fd/...")
        val raf = try { RandomAccessFile(binary, "r") } catch (_: Exception) { null }
        if (raf != null) {
            try {
                val fdField = FileDescriptor::class.java.getDeclaredField("descriptor")
                fdField.isAccessible = true
                val fd = fdField.getInt(raf.fd)
                val fdPath = "/proc/self/fd/$fd"
                if (tryMethod { execDirect(File(fdPath), emptyList(), workDir).apply { destroy(); waitFor() } }) {
                    raf.close()
                    DebugLog.log("Exec", "Probe: PROC_SELF_FD works for ${binary.name}")
                    return ExecMethod.PROC_SELF_FD
                }
            } catch (_: Exception) { }
            raf.close()
        }

        DebugLog.log("Exec", "Probe: /proc/self/fd failed, trying linker64 for ${binary.name}")
        Log.i(TAG, "/proc/self/fd failed, trying linker64...")
        if (tryMethod { execLinker64(binary, emptyList(), workDir).apply { destroy(); waitFor() } }) {
            DebugLog.log("Exec", "Probe: LINKER64 works for ${binary.name}")
            return ExecMethod.LINKER64
        }

        DebugLog.log("Exec", "Probe: ALL methods failed for ${binary.name}")
        throw RuntimeException("Cannot execute binary: ${binary.absolutePath} (noexec mount)")
    }

    private fun execDirect(binary: File, args: List<String>, workDir: String, extraEnv: Map<String, String> = emptyMap()): Process {
        val pb = ProcessBuilder(listOf(binary.absolutePath) + args)
        pb.directory(File(workDir))
        pb.environment()["LD_LIBRARY_PATH"] = workDir
        pb.environment().putAll(extraEnv)
        pb.redirectErrorStream(true)
        return pb.start()
    }

    private fun execProcSelfFd(binary: File, args: List<String>, workDir: String, extraEnv: Map<String, String> = emptyMap()): Process {
        val raf = RandomAccessFile(binary, "r")
        val fdField = FileDescriptor::class.java.getDeclaredField("descriptor")
        fdField.isAccessible = true
        val fd = fdField.getInt(raf.fd)
        val fdPath = "/proc/self/fd/$fd"

        val pb = ProcessBuilder(listOf(fdPath) + args)
        pb.directory(File(workDir))
        pb.environment()["LD_LIBRARY_PATH"] = workDir
        pb.environment().putAll(extraEnv)
        pb.redirectErrorStream(true)
        val process = pb.start()

        // Keep raf open while process runs
        ProcessHolder.hold(raf, process)
        return process
    }

    private fun execLinker64(binary: File, args: List<String>, workDir: String, extraEnv: Map<String, String> = emptyMap()): Process {
        val is64 = android.os.Build.SUPPORTED_64_BIT_ABIS?.isNotEmpty() ?: true
        val linker = if (is64) "/system/bin/linker64" else "/system/bin/linker"
        val pb = ProcessBuilder(listOf(linker, binary.absolutePath) + args)
        pb.directory(File(workDir))
        pb.environment()["LD_LIBRARY_PATH"] = workDir
        pb.environment().putAll(extraEnv)
        pb.redirectErrorStream(true)
        return pb.start()
    }

    private fun tryMethod(block: () -> Any?): Boolean {
        return try {
            block()
            true
        } catch (_: Exception) {
            false
        }
    }

    private object ProcessHolder {
        private val holders = mutableMapOf<Process, AutoCloseable>()

        fun hold(closeable: AutoCloseable, process: Process) {
            synchronized(holders) {
                holders[process] = closeable
            }
            Thread {
                process.waitFor()
                synchronized(holders) {
                    holders.remove(process)?.close()
                }
            }.apply { isDaemon = true }.start()
        }
    }
}
