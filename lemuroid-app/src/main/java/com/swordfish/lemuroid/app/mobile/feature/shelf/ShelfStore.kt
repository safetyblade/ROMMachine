package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Versioned, atomic presentation file; Room, ROMs, URI grants and saves are never written. */
class ShelfStore(context: Context) {
    private val file = File(context.filesDir, "cartdeck/shelf-layout-v1.json")
    private val atomic = AtomicFile(file)
    private val lock = locks.getOrPut(file.absolutePath) { Mutex() }

    companion object {
        private val locks = ConcurrentHashMap<String, Mutex>()
    }

    suspend fun read(): ShelfLayout = withContext(Dispatchers.IO) { lock.withLock { readLocked() } }

    suspend fun update(transform: (ShelfLayout) -> ShelfLayout): Pair<ShelfLayout, ShelfLayout> =
        withContext(Dispatchers.IO + NonCancellable) {
            lock.withLock {
                val before = readLocked()
                val changed = transform(before).validate()
                if (changed == before) return@withLock before to before
                val after = changed.copy(revision = before.revision + 1)
                file.parentFile!!.mkdirs()
                val stream = atomic.startWrite()
                try {
                    stream.write(encode(after).toByteArray(Charsets.UTF_8))
                    atomic.finishWrite(stream)
                } catch (error: Throwable) {
                    atomic.failWrite(stream)
                    throw error
                }
                before to after
            }
        }

    private fun readLocked(): ShelfLayout {
        if (!file.exists() && !File(file.path + ".bak").exists()) return ShelfLayout()
        // A corrupt/future file is an actionable error, never silently reset and overwritten.
        return decode(atomic.openRead().bufferedReader().use { it.readText() })
    }
}

internal fun encode(layout: ShelfLayout): String =
    JSONObject().apply {
        put("version", layout.version)
        put("revision", layout.revision)
        put(
            "appearance",
            JSONObject().apply {
                put("edition", layout.appearance.edition)
                put("highContrast", layout.appearance.highContrast)
                put("reducedMotion", layout.appearance.reducedMotion)
                put("custom", JSONObject(layout.appearance.custom))
                put("appearanceVersion", 3)
                put("activeProfileId", layout.appearance.activeProfileId ?: JSONObject.NULL)
                put("curatedDesignId", layout.appearance.curatedDesignId ?: JSONObject.NULL)
                put(
                    "profiles",
                    JSONArray().apply {
                        layout.appearance.profiles.forEach { profile ->
                            put(
                                JSONObject().apply {
                                    put("id", profile.id)
                                    put("name", profile.name)
                                    put("baseEdition", profile.baseEdition)
                                    put("custom", JSONObject(profile.custom))
                                    put("createdAt", profile.createdAt)
                                    put("updatedAt", profile.updatedAt)
                                },
                            )
                        }
                    },
                )
            },
        )
        put(
            "sections",
            JSONArray().apply {
                layout.sections.forEach { section ->
                    put(
                        JSONObject().apply {
                            put("id", section.id)
                            put("title", section.title)
                            put(
                                "cells",
                                JSONArray().apply {
                                    section.cells.forEach { cell ->
                                        put(
                                            JSONObject().apply {
                                                put("id", cell.id)
                                                put("gameKey", cell.gameKey ?: JSONObject.NULL)
                                                put("titleHint", cell.titleHint)
                                                put("emptyRow", cell.emptyRow)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                    )
                }
            },
        )
    }.toString()

internal fun decode(json: String): ShelfLayout {
    val root = JSONObject(json)
    require(root.getInt("version") == 1) { "Unsupported shelf format" }
    val appearance = root.getJSONObject("appearance")
    val custom = appearance.optJSONObject("custom") ?: JSONObject()
    val profiles = appearance.optJSONArray("profiles") ?: JSONArray()
    require(appearance.optInt("appearanceVersion", 1) in 1..3) { "Unknown appearance schema; retaining stored file" }
    val sections = root.getJSONArray("sections")
    return ShelfLayout(
        version = root.getInt("version"),
        revision = root.getLong("revision"),
        appearance =
            ShelfAppearance(
                edition = appearance.getString("edition"),
                highContrast = appearance.optBoolean("highContrast"),
                reducedMotion = appearance.optBoolean("reducedMotion"),
                custom = custom.keys().asSequence().associateWith { custom.getLong(it) },
                profiles =
                    (0 until profiles.length()).map { index ->
                        val record = profiles.getJSONObject(index)
                        val overrides = record.optJSONObject("custom") ?: JSONObject()
                        NamedAppearance(
                            id = record.getString("id"),
                            name = record.getString("name"),
                            baseEdition = record.getString("baseEdition"),
                            custom = overrides.keys().asSequence().associateWith { overrides.getLong(it) },
                            createdAt = record.optLong("createdAt"),
                            updatedAt = record.optLong("updatedAt"),
                        )
                    },
                activeProfileId =
                    if (appearance.isNull(
                            "activeProfileId",
                        )
                    ) {
                        null
                    } else {
                        appearance.getString("activeProfileId")
                    },
                curatedDesignId =
                    if (appearance.isNull("curatedDesignId")) {
                        null
                    } else {
                        appearance.getString("curatedDesignId")
                    },
            ),
        sections =
            (0 until sections.length()).map { index ->
                val section = sections.getJSONObject(index)
                val cells = section.getJSONArray("cells")
                ShelfSection(
                    section.getString("id"),
                    section.getString("title"),
                    (0 until cells.length()).map { cellIndex ->
                        val cell = cells.getJSONObject(cellIndex)
                        ShelfCell(
                            cell.getString("id"),
                            if (cell.isNull("gameKey")) null else cell.getString("gameKey"),
                            cell.optString("titleHint"),
                            cell.optBoolean("emptyRow"),
                        )
                    },
                )
            },
    ).validate()
}
