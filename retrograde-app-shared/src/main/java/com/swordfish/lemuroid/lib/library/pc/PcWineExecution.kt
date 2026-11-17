package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.security.MessageDigest

/**
 * PC-W1: explicit Wine executables only. This is not a DOS auto-scan classifier.
 * All inputs are app-managed, and the native runner is a packaged executable.
 */
object PcWineExecution {
    enum class Architecture { X86, X64 }

    fun inspectExecutable(file: File): Architecture {
        RandomAccessFile(file, "r").use { input ->
            require(input.length() >= 64 && input.readUnsignedShort() == 0x4d5a) { "Not a Windows executable" }
            input.seek(60)
            val offset = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            require(offset in 64..(input.length() - 24)) { "Invalid PE header" }
            input.seek(offset)
            require(input.readInt() == 0x50450000) { "Invalid PE signature" }
            return when (java.lang.Short.reverseBytes(input.readShort()).toInt() and 0xffff) {
                0x014c -> Architecture.X86
                0x8664 -> Architecture.X64
                else -> error("Unsupported Windows executable architecture")
            }
        }
    }

    data class Command(
        val arguments: List<String>,
        val environment: Map<String, String>,
        val workingDirectory: File,
    )

    /**
     * The immutable package receipt must be supplied by the verified native
     * producer. An arbitrary user ZIP or downloaded Winlator APK is not a runtime.
     */
    class Runtime(
        private val nativeLibraryDirectory: File,
        private val rootfs: File,
        private val receipt: Map<String, String>,
    ) {
        private fun verified(
            file: File,
            root: File,
            key: String,
        ): File {
            val canonical = file.canonicalFile
            check(canonical.toPath().startsWith(root.canonicalFile.toPath()) && canonical.isFile)
            val expected = checkNotNull(receipt[key]) { "Unverified Wine component: $key" }
            check(expected.matches(Regex("[a-f0-9]{64}")) && sha256(canonical) == expected) {
                "Wine component verification failed: $key"
            }
            return canonical
        }

        fun command(
            workspace: PcWorkspace,
            executable: File,
            display: String,
            sharedMemorySocket: File,
        ): Command {
            require(display.matches(Regex(":[0-9]{1,3}")))
            val runner =
                verified(File(nativeLibraryDirectory, "libcartdeck_box64.so"), nativeLibraryDirectory, "box64")
            check(runner.canExecute()) { "Packaged CPU translator is not executable" }
            val wine = verified(File(rootfs, "opt/wine/bin/wine"), rootfs, "wine")
            val architecture = inspectExecutable(executable)
            if (architecture == Architecture.X86) {
                check(receipt["wow64"] == "verified") { "32-bit Wine WoW64 is not verified" }
            }
            val runtime = workspace.runtime.canonicalFile
            val content = File(runtime, "content").canonicalFile
            val program = executable.canonicalFile
            require(program.toPath().startsWith(content.toPath())) { "Executable is outside the PC workspace" }
            val socket = sharedMemorySocket.canonicalFile
            check(socket.toPath().startsWith(runtime.toPath()) && socket.exists()) { "Display host is not ready" }

            fun directory(name: String): File =
                File(runtime, name).apply { check(isDirectory || mkdirs()) }.canonicalFile.also {
                    check(it.toPath().startsWith(runtime.toPath()))
                }

            val prefix = directory("wineprefix")
            val home = directory("home")
            val tmp = directory("tmp")
            return Command(
                listOf(runner.path, wine.path, program.path),
                mapOf(
                    "HOME" to home.path,
                    "TMPDIR" to tmp.path,
                    "WINEPREFIX" to prefix.path,
                    "DISPLAY" to display,
                    "ANDROID_SYSVSHM_SERVER" to socket.path,
                    "PATH" to File(rootfs, "opt/wine/bin").path + ":" + File(rootfs, "usr/bin").path,
                    "LD_LIBRARY_PATH" to File(rootfs, "lib").path,
                    "BOX64_LD_LIBRARY_PATH" to File(rootfs, "lib/x86_64-linux-gnu").path,
                    "BOX64_DYNAREC" to "1",
                    "WINEDEBUG" to "-all",
                ),
                checkNotNull(program.parentFile),
            )
        }
    }

    /** No interpolation into a shell command; paths and executable are distinct argv entries. */
    fun start(
        command: Command,
        log: File,
    ): Process {
        check(log.parentFile?.isDirectory == true)
        check(!Files.isSymbolicLink(log.toPath()))
        return ProcessBuilder(command.arguments)
            .directory(command.workingDirectory)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.to(log))
            .apply {
                environment().clear()
                environment().putAll(command.environment)
            }
            .start()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
