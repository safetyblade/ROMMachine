package com.swordfish.lemuroid.app.shared.game.xbox360

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom

/** Persistent offline account provisioning for Xenia's existing ProfileManager. */
object Xbox360Profiles {
    const val ASSETS = "cartdeck/xenia-profile"
    private val xuidPattern = Regex("[0-9A-F]{16}")

    data class Selection(val xuid: String, val created: Boolean)

    fun prepare(
        context: Context,
        data: File,
    ): Selection {
        val proof =
            context.assets.open(
                "$ASSETS/profile-source.json",
            ).bufferedReader().use { JSONObject(it.readText()) }
        check(
            proof.getString("revision") == Xbox360Engine.REVISION && proof.getBoolean("offline_only") &&
                proof.getBoolean("native_encrypt_decrypt_verified") && !proof.getBoolean("xuid_bound"),
        ) {
            "Xbox 360 offline profile provenance is unavailable"
        }
        val template = context.assets.open("$ASSETS/Account").use { it.readBytes() }
        return provision(data, template, proof.getString("sha256"))
    }

    internal fun account(
        data: File,
        xuid: String,
    ): File {
        require(xuidPattern.matches(xuid) && xuid != "0000000000000000")
        val content = File(data, "content").canonicalFile
        val profile = File(content, "$xuid/FFFE07D1/00010000/$xuid/Account").canonicalFile
        check(profile.path.startsWith(content.path + File.separator)) { "Xbox 360 profile escaped private storage" }
        return profile
    }

    internal fun provision(
        data: File,
        template: ByteArray,
        expectedHash: String,
    ): Selection {
        check(template.size == 0x194 && hash(template) == expectedHash) { "Xbox 360 offline account hash mismatch" }
        val content = File(data, "content").apply { check(isDirectory || mkdirs()) }
        val marker = AtomicFile(File(data, "profile/player-one.xuid"))

        fun valid(xuid: String) = account(data, xuid).let { it.isFile && it.length() >= 0x194 }

        fun bind(
            xuid: String,
            created: Boolean,
        ): Selection {
            val output = marker.startWrite()
            try {
                output.write(xuid.toByteArray(Charsets.US_ASCII))
                marker.finishWrite(output)
            } catch (error: Throwable) {
                marker.failWrite(output)
                throw error
            }
            return Selection(xuid, created)
        }
        // Respect an existing explicit native profile selection; never edit its config or account.
        val config = File(data, "config/xenia.config.toml")
        val configured =
            if (config.isFile) {
                Regex("(?m)^\\s*logged_profile_slot_0_xuid\\s*=\\s*[\"']([0-9a-fA-F]{16})[\"']")
                    .find(config.readText())?.groupValues?.get(1)?.uppercase()
            } else {
                null
            }
        if (configured != null) {
            check(
                valid(configured),
            ) { "Configured Xbox 360 profile is missing; restore its Account file before reopening" }
            return bind(configured, false)
        }
        val saved = runCatching { marker.openRead().bufferedReader().use { it.readText().trim() } }.getOrNull()
        if (saved != null) {
            check(xuidPattern.matches(saved) && valid(saved)) {
                "Xbox 360 player profile is missing; restore its Account file before reopening"
            }
            return Selection(saved, false)
        }
        val existing =
            content.listFiles().orEmpty().filter { it.isDirectory && xuidPattern.matches(it.name) }
                .map { it.name }.filter { it != "0000000000000000" && valid(it) }.sorted().firstOrNull()
        if (existing != null) return bind(existing, false)
        val random = SecureRandom()
        val xuid =
            generateSequence {
                "E0300000" + (random.nextInt() and Int.MAX_VALUE).toString(16).uppercase().padStart(8, '0')
            }.take(32).firstOrNull { !File(content, it).exists() } ?: error("Offline profile identity unavailable")
        val target = account(data, xuid)
        check(target.parentFile!!.mkdirs())
        val pending = File(target.parentFile, ".Account.pending")
        FileOutputStream(pending).use { output ->
            output.write(template)
            output.fd.sync()
        }
        check(pending.renameTo(target)) { "Could not retain the Xbox 360 offline profile" }
        return bind(xuid, true)
    }

    internal fun hash(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}
