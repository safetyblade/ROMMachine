package com.swordfish.lemuroid.app.mobile.feature.shelf

import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/** Presentation state only. Never use cells as ROM, game or save records. */
data class ShelfCell(
    val id: String = UUID.randomUUID().toString(),
    val gameKey: String? = null,
    val titleHint: String = "",
    val emptyRow: Boolean = false,
)

data class ShelfSection(val id: String, val title: String, val cells: List<ShelfCell> = emptyList())

/** Sort mode changes only shelf presentation; not ROM identity or saved game data. */
enum class ShelfArrangeMode { TITLE_A_TO_Z, CONSOLE_THEN_TITLE }

/** A named colour-only profile. Never a game, ROM, library or save record. */
data class NamedAppearance(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseEdition: String,
    val custom: Map<String, Long> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)

/**
 * V1 edition/custom fields remain the applied palette and continue to be
 * readable from existing shelf-layout-v1.json installs. The optional V2
 * profiles are stored in that SAME AtomicFile, so applying and saving names
 * is a single transaction and never touches the cartridge positions.
 */
data class ShelfAppearance(
    val edition: String = "original",
    val custom: Map<String, Long> = emptyMap(),
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val profiles: List<NamedAppearance> = emptyList(),
    val activeProfileId: String? = null,
) {
    fun choosePreset(id: String): ShelfAppearance {
        require(id in listOf("original", "pink", "green"))
        return copy(edition = id, custom = emptyMap(), activeProfileId = null)
    }

    fun selectProfile(id: String): ShelfAppearance {
        val profile = profiles.first { it.id == id }
        return copy(edition = profile.baseEdition, custom = profile.custom, activeProfileId = id)
    }

    fun removeProfile(id: String): ShelfAppearance =
        copy(
            profiles = profiles.filterNot { it.id == id },
            activeProfileId = if (activeProfileId == id) null else activeProfileId,
        )

    fun saveNewProfile(
        name: String,
        now: Long = System.currentTimeMillis(),
    ): ShelfAppearance {
        val title = name.trim()
        require(title.isNotEmpty() && title.length <= 60)
        val profile =
            NamedAppearance(name = title, baseEdition = edition, custom = custom, createdAt = now, updatedAt = now)
        return copy(profiles = profiles + profile, activeProfileId = profile.id)
    }

    fun renameProfile(
        id: String,
        name: String,
    ): ShelfAppearance {
        val title = name.trim()
        require(title.isNotEmpty() && title.length <= 60)
        return copy(profiles = profiles.map { if (it.id == id) it.copy(name = title) else it })
    }

    fun duplicateProfile(
        id: String,
        now: Long = System.currentTimeMillis(),
    ): ShelfAppearance {
        val source = profiles.first { it.id == id }
        val duplicate =
            source.copy(
                id = UUID.randomUUID().toString(),
                name = (source.name.take(50) + " Copy"),
                createdAt = now,
                updatedAt = now,
            )
        return copy(
            profiles = profiles + duplicate,
            edition = duplicate.baseEdition,
            custom = duplicate.custom,
            activeProfileId = duplicate.id,
        )
    }
}

data class ShelfLayout(
    val version: Int = 1,
    val revision: Long = 0,
    val sections: List<ShelfSection> = listOf(ShelfSection(UNSORTED, "Unsorted")),
    val appearance: ShelfAppearance = ShelfAppearance(),
) {
    companion object {
        const val UNSORTED = "unsorted"
    }

    fun validate(): ShelfLayout {
        require(version == 1) { "Unsupported shelf format $version" }
        require(sections.any { it.id == UNSORTED })
        require(sections.map { it.id }.distinct().size == sections.size)
        require(sections.all { it.title.isNotBlank() && it.title.length <= 80 })
        require(appearance.edition in listOf("original", "pink", "green"))
        require(appearance.profiles.map { it.id }.distinct().size == appearance.profiles.size)
        require(
            appearance.profiles.all {
                it.name.isNotBlank() && it.name.length <= 60 &&
                    it.baseEdition in listOf("original", "pink", "green")
            },
        )
        require(
            appearance.activeProfileId == null ||
                appearance.profiles.any { it.id == appearance.activeProfileId },
        )
        val cells = sections.flatMap { it.cells }
        require(cells.map { it.id }.distinct().size == cells.size)
        require(cells.mapNotNull { it.gameKey }.distinct().size == cells.count { it.gameKey != null })
        require(cells.none { it.emptyRow && it.gameKey != null })
        return this
    }

    /**
     * The normal UI reconciliation retains missing items while indexing is running.
     * Only a completely successful folder scan may remove obsolete game-linked cells.
     * User-created vacant slots/empty rows are never pruned or moved by that cleanup.
     */
    fun reconcile(
        games: Map<String, String>,
        removeMissing: Boolean = false,
    ): ShelfLayout {
        val existing = sections.flatMap { it.cells }.mapNotNull { it.gameKey }.toSet()
        val additions =
            games.filterKeys { it !in existing }.map { (key, title) ->
                ShelfCell(id = "game:$key", gameKey = key, titleHint = title)
            }
        return copy(
            sections =
                sections.map { section ->
                    section.copy(
                        cells =
                            section.cells
                                .filter { cell -> !removeMissing || cell.gameKey == null || cell.gameKey in games }
                                .map { cell ->
                                    cell.copy(titleHint = games[cell.gameKey] ?: cell.titleHint)
                                } + if (section.id == UNSORTED) additions else emptyList(),
                    )
                },
        ).validate()
    }

    fun createSection(title: String): ShelfLayout =
        copy(sections = sections + ShelfSection(UUID.randomUUID().toString(), checkedTitle(title))).validate()

    fun renameSection(
        id: String,
        title: String,
    ): ShelfLayout =
        copy(sections = sections.map { if (it.id == id) it.copy(title = checkedTitle(title)) else it }).validate()

    fun moveSection(
        id: String,
        delta: Int,
    ): ShelfLayout {
        val list = sections.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        require(index >= 0)
        val target = (index + delta).coerceIn(list.indices)
        list.add(target, list.removeAt(index))
        return copy(sections = list).validate()
    }

    fun deleteSection(id: String): ShelfLayout {
        require(id != UNSORTED) { "Unsorted is the safe home for rehomed cartridges" }
        val removed = sections.first { it.id == id }
        return copy(
            sections =
                sections.filterNot { it.id == id }.map {
                    if (it.id == UNSORTED) it.copy(cells = it.cells + removed.cells) else it
                },
        ).validate()
    }

    fun insertSpace(
        sectionId: String,
        beforeCellId: String? = null,
        row: Boolean = false,
    ): ShelfLayout =
        copy(
            sections =
                sections.map { section ->
                    if (section.id != sectionId) {
                        section
                    } else {
                        val list = section.cells.toMutableList()
                        val target = beforeCellId?.let { id -> list.indexOfFirst { it.id == id } } ?: list.size
                        require(target >= 0)
                        list.add(target, ShelfCell(emptyRow = row))
                        section.copy(cells = list)
                    }
                },
        ).validate()

    /** Drop on a vacancy swaps it with the source; gaps never collapse accidentally. */
    fun moveCell(
        cellId: String,
        sectionId: String,
        beforeCellId: String? = null,
    ): ShelfLayout {
        val sourceSection = sections.first { section -> section.cells.any { it.id == cellId } }
        val source = sourceSection.cells.first { it.id == cellId }
        require(sections.any { it.id == sectionId })
        if (cellId == beforeCellId) return this
        val destination = sections.first { it.id == sectionId }.cells.firstOrNull { it.id == beforeCellId }
        if (destination != null && destination.gameKey == null && !destination.emptyRow) {
            return copy(
                sections =
                    sections.map { section ->
                        section.copy(
                            cells =
                                section.cells.map { cell ->
                                    when (cell.id) {
                                        source.id -> destination
                                        destination.id -> source
                                        else -> cell
                                    }
                                },
                        )
                    },
            ).validate()
        }
        val without = sections.map { it.copy(cells = it.cells.filterNot { cell -> cell.id == cellId }) }
        return copy(
            sections =
                without.map { section ->
                    if (section.id != sectionId) {
                        section
                    } else {
                        val list = section.cells.toMutableList()
                        val target = beforeCellId?.let { id -> list.indexOfFirst { it.id == id } } ?: list.size
                        require(target >= 0)
                        list.add(target, source)
                        section.copy(cells = list)
                    }
                },
        ).validate()
    }

    /** Explicitly close every shelf gap/blank row, preserving original game and section order. */
    fun removeAllSpaces(): ShelfLayout =
        copy(
            sections = sections.map { section ->
                section.copy(cells = section.cells.filter { it.gameKey != null })
            },
        ).validate()

    /** Deterministic in-section reordering. Game keys, cells and sections are retained. */
    fun autoArrange(
        mode: ShelfArrangeMode,
        systemsByGameKey: Map<String, String> = emptyMap(),
    ): ShelfLayout {
        val compact = removeAllSpaces()
        val byTitle = compareBy<ShelfCell>(
            { it.titleHint.trim().lowercase(Locale.ROOT) },
            { it.gameKey.orEmpty() },
        )
        val comparator =
            when (mode) {
                ShelfArrangeMode.TITLE_A_TO_Z -> byTitle
                ShelfArrangeMode.CONSOLE_THEN_TITLE ->
                    compareBy<ShelfCell> {
                        systemsByGameKey[it.gameKey.orEmpty()].orEmpty().lowercase(Locale.ROOT)
                    }.then(byTitle)
            }
        return compact.copy(
            sections = compact.sections.map { it.copy(cells = it.cells.sortedWith(comparator)) },
        ).validate()
    }

    fun removeSpace(cellId: String): ShelfLayout {
        val cell = sections.flatMap { it.cells }.first { it.id == cellId }
        require(cell.gameKey == null) { "Cartridges cannot be deleted through shelf editing" }
        return copy(
            sections =
                sections.map {
                    it.copy(
                        cells =
                            it.cells.filterNot {
                                    cell ->
                                cell.id == cellId
                            },
                    )
                },
        ).validate()
    }

    private fun checkedTitle(title: String): String = title.trim().also { require(it.isNotEmpty() && it.length <= 80) }
}

/** Stable through Room ID churn. This hashes identity; it never rewrites the source URI. */
fun shelfGameKey(
    system: String,
    fileUri: String,
): String =
    MessageDigest.getInstance("SHA-256")
        .digest((system + "\u0000" + fileUri).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/** Explicit empty rows remain full rows at every screen width. */
data class ShelfRow(
    val key: String,
    val sectionId: String,
    val cells: List<ShelfCell>,
    val intentionalEmpty: Boolean = false,
)

fun ShelfSection.rows(columns: Int): List<ShelfRow> {
    require(columns >= 1)
    val rows = mutableListOf<ShelfRow>()
    val pending = mutableListOf<ShelfCell>()

    fun flush() {
        if (pending.isNotEmpty()) {
            rows += ShelfRow("$id:${pending.first().id}", id, pending.toList())
            pending.clear()
        }
    }
    cells.forEach { cell ->
        if (cell.emptyRow) {
            flush()
            rows += ShelfRow("$id:${cell.id}", id, listOf(cell), intentionalEmpty = true)
        } else {
            pending += cell
            if (pending.size == columns) flush()
        }
    }
    flush()
    return rows
}
