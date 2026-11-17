package com.swordfish.lemuroid.app.shared.game.pc

import android.content.Context
import com.swordfish.lemuroid.lib.library.pc.PcWineExecution
import com.swordfish.lemuroid.lib.library.pc.PcWorkspace
import java.io.File

/**
 * Native Wine execution boundary for a future verified X11/audio host.
 * Intentionally not registered as a selectable engine before native proof.
 */
internal class PcWineRuntime(private val context: Context) {
    fun launchVerified(
        workspace: PcWorkspace,
        executable: File,
        display: String,
        sharedMemorySocket: File,
        trustedReceipt: Map<String, String>,
    ): Process {
        check(trustedReceipt["distribution"] == "cleared") { "Wine distribution is not cleared" }
        check(trustedReceipt["android-load"] == "verified") { "Android Wine host is not verified" }
        check(trustedReceipt["graphics-host"] == "verified") { "Wine display host is not verified" }
        val runtime =
            PcWineExecution.Runtime(
                File(context.applicationInfo.nativeLibraryDir),
                File(context.filesDir, "pc-wine-runtime/rootfs"),
                trustedReceipt,
            )
        val command = runtime.command(workspace, executable, display, sharedMemorySocket)
        return PcWineExecution.start(command, File(workspace.runtime, "wine-session.log"))
    }
}
