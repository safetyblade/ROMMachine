package com.swordfish.lemuroid.app.shared.game.engine

import android.content.Context
import android.util.AtomicFile
import com.swordfish.lemuroid.app.shared.game.EmulatorCompatibilityPreferences
import com.swordfish.lemuroid.lib.BuildConfig
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Properties

/** Preferences survive install-over. Runtime outcomes are read from disk across the separate game processes. */
class EngineSelectionPreferences(private val context: Context) {
    class SelectionException(message: String) : IllegalStateException(message)

    enum class FallbackPolicy { ASK, NEXT_MANUAL_LAUNCH, NEVER }

    data class Selection(
        val engine: NativeEngineRegistry.Engine,
        val changedEngine: Boolean,
        val reason: String,
    )

    private val preferences = context.getSharedPreferences("cartdeck_engine_registry_v1", Context.MODE_PRIVATE)

    fun preferred(
        system: SystemID,
        game: Game? = null,
    ): String? =
        game?.let { preferences.getString(gameKey(it), null) }
            ?: preferences.getString("system_${system.dbname}", null)

    fun setPreferred(
        system: GameSystem,
        engineId: String?,
        game: Game? = null,
    ): Boolean {
        if (engineId != null) {
            val engine = NativeEngineRegistry.engines(system).firstOrNull { it.id == engineId } ?: return false
            if (!NativeEngineRegistry.eligibility(context, engine).eligible) return false
        }
        val key = game?.let(::gameKey) ?: "system_${system.id.dbname}"
        val edit = preferences.edit()
        edit.putString(key, engineId ?: AUTOMATIC)
        val committed = edit.commit()
        if (committed && game != null) {
            val event = outcome(game)
            if (engineId == null) {
                event.stringPropertyNames().filter { it.startsWith("failed.") }.forEach { event.remove(it) }
                event.remove("successful")
            } else {
                event.remove("failed.$engineId")
            }
            event.remove("failureStage")
            writeOutcome(game, event)
        }
        return committed
    }

    fun inheritConsole(game: Game): Boolean = preferences.edit().remove(gameKey(game)).commit()

    fun fallbackPolicy(system: SystemID): FallbackPolicy =
        runCatching {
            FallbackPolicy.valueOf(preferences.getString("fallback_${system.dbname}", FallbackPolicy.ASK.name)!!)
        }.getOrDefault(FallbackPolicy.ASK)

    fun setFallbackPolicy(
        system: SystemID,
        policy: FallbackPolicy,
    ): Boolean = preferences.edit().putString("fallback_${system.dbname}", policy.name).commit()

    /** This never launches an alternative: a deliberate Open is required for every attempt. */
    fun select(
        game: Game,
        legacy: SystemCoreConfig?,
    ): Selection {
        val system = GameSystem.findById(game.systemId)
        val engines = NativeEngineRegistry.engines(system)
        val event = outcome(game)
        val compatibility = EmulatorCompatibilityPreferences(context)
        val migratedChoice =
            if (compatibility.isUpdatedRequested(
                    system.id,
                    game.id,
                )
            ) {
                compatibility.candidateCore(system.id)?.coreName
            } else {
                null
            }
        val savedPreference = preferred(system.id, game)
        val explicit =
            savedPreference?.takeUnless { it == AUTOMATIC } ?: migratedChoice.takeIf { savedPreference == null }
        val stable = event.getProperty("successful")
        val proven = listOfNotNull(stable, event.getProperty("previous"))
        val default = legacy?.coreID?.coreName ?: engines.firstOrNull()?.id
        val requested = explicit ?: stable ?: default
        val engine =
            engines.firstOrNull { it.id == requested }
                ?: throw SelectionException("Engine unavailable. Choose an installed engine in cartridge settings.")
        // ZIP membership is validated by the existing archive loader after extraction.
        val extension =
            if (engine.manifest != null) {
                game.fileName.substringAfterLast('.', "").lowercase().takeUnless {
                    it == "zip"
                }
            } else {
                null
            }
        val eligibility = NativeEngineRegistry.eligibility(context, engine, extension)
        if (!eligibility.eligible) throw SelectionException(eligibility.reason ?: "The selected engine is unavailable")
        val failed = event.getProperty("failed.${engine.id}") == identity(engine)
        if (failed) {
            val alternative =
                if (fallbackPolicy(system.id) == FallbackPolicy.NEXT_MANUAL_LAUNCH) {
                    // Only an established, previously successful engine is eligible for unattended fallback.
                    engines.firstOrNull {
                        it.id in proven && it.id != engine.id &&
                            NativeEngineRegistry.eligibility(context, it).eligible &&
                            event.getProperty("failed.${it.id}") != identity(it)
                    }
                } else {
                    null
                }
            if (alternative == null) {
                throw SelectionException(
                    "Engine stopped at ${event.getProperty(
                        "failureStage",
                        "startup",
                    )}. Choose another installed engine or explicitly retry in cartridge settings.",
                )
            }
            return Selection(alternative, true, "Previous successful engine selected after a confirmed failure")
        }
        val prior = event.getProperty("lastSelected")
        return Selection(
            engine,
            prior != null && prior != engine.id,
            if (explicit != null) "Preferred engine" else "Automatic stable selection",
        )
    }

    fun beginLaunch(
        game: Game,
        engine: NativeEngineRegistry.Engine,
    ) {
        val event = outcome(game)
        event.setProperty("lastSelected", engine.id)
        event.setProperty("selectedIdentity", identity(engine))
        event.setProperty("stage", "CORE_SELECT")
        writeOutcome(game, event)
    }

    /** Accepted submission/presentation is evidence, not a proven compatible engine. */
    fun recordPresentation(game: Game) {
        val event = outcome(game)
        val selected = event.getProperty("selectedIdentity") ?: return
        event.setProperty("presentationAccepted", selected)
        event.setProperty("stage", "PRESENT_ACCEPTED")
        // Do not clear failures, rotate successful/previous engines or change preferences.
        writeOutcome(game, event)
    }

    /** Called only after an observed graphical frame, never after activity creation or a timeout. */
    fun recordSuccess(game: Game) {
        // hakuX's existing callback only proves a VkQueuePresent result, even
        // when every sampled guest pixel is white. It cannot promote fallback proof.
        if (game.systemId in setOf(SystemID.XBOX.dbname, SystemID.XBOX360.dbname)) {
            recordPresentation(game)
            return
        }
        val event = outcome(game)
        val selected = event.getProperty("lastSelected") ?: return
        val previous = event.getProperty("successful")
        if (previous != null && previous != selected) event.setProperty("previous", previous)
        event.setProperty("successful", selected)
        event.setProperty("stage", "FIRST_FRAME")
        event.remove("failed.$selected")
        event.remove("failureStage")
        writeOutcome(game, event)
    }

    /** Stage strings are bounded symbolic diagnostics; never store ROM names, paths or arbitrary exception text. */
    fun recordFailure(
        game: Game,
        stage: String,
    ) {
        require(stage.matches(Regex("[A-Z_]{1,40}")))
        val event = outcome(game)
        val selected = event.getProperty("selectedIdentity") ?: return
        event.setProperty("failed.${event.getProperty("lastSelected")}", selected)
        event.setProperty("failureStage", stage)
        event.setProperty("stage", stage)
        writeOutcome(game, event)
    }

    fun previous(game: Game): String? = outcome(game).getProperty("previous")

    fun failureStage(game: Game): String? = outcome(game).getProperty("failureStage")

    private fun identity(engine: NativeEngineRegistry.Engine): String {
        val host = BuildConfig.NATIVE_HOST_VERSION
        val version =
            NativeEngineRegistry.version(context, engine) ?: "apk-${com.swordfish.lemuroid.BuildConfig.VERSION_CODE}"
        return "${engine.id}:$version:$host"
    }

    private fun gameKey(game: Game) = "game_${game.systemId}_${digest(game.fileUri)}"

    private fun outcomeFile(game: Game): AtomicFile =
        AtomicFile(File(context.filesDir, "native-engine-outcomes/${gameKey(game)}.properties"))

    private fun outcome(game: Game): Properties =
        withOutcomeLock(game) {
            Properties().apply {
                // Read the committed base only. AtomicFile.openRead() removes .new and
                // can destroy an in-flight write by the separate native process.
                val base = outcomeFile(game).baseFile
                val backup = File("${base.path}.bak")
                val committed = if (backup.isFile) backup else base
                if (committed.isFile) committed.inputStream().use { load(it) }
            }
        }

    private fun <T> withOutcomeLock(
        game: Game,
        block: () -> T,
    ): T =
        synchronized(OUTCOME_LOCK) {
            val base = outcomeFile(game).baseFile
            base.parentFile?.mkdirs()
            RandomAccessFile(File(base.parentFile, "${base.name}.lock"), "rw").use { file ->
                file.channel.lock().use { block() }
            }
        }

    private fun writeOutcome(
        game: Game,
        values: Properties,
    ) = withOutcomeLock(game) {
        val file = outcomeFile(game)
        file.baseFile.parentFile?.mkdirs()
        val output = file.startWrite()
        try {
            values.store(output, "CARTDECK engine outcome v1")
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    companion object {
        const val AUTOMATIC = "automatic"
        private val OUTCOME_LOCK = Any()
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance(
            "SHA-256",
        ).digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
