package com.swordfish.lemuroid.app.mobile.feature.product

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.mods.ModFiles
import com.swordfish.lemuroid.lib.saves.CartridgeSaveBinding
import com.swordfish.lemuroid.lib.saves.PlayStationMemoryCard
import com.swordfish.lemuroid.lib.saves.safesave.BoundSaveRestore
import com.swordfish.lemuroid.lib.saves.safesave.CartSaveArchive
import com.swordfish.lemuroid.lib.saves.safesave.SaveStorageLease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@Composable
internal fun SafeSaveScreen(
    games: List<Game>,
    binding: CartridgeSaveBinding,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inventory by remember { mutableStateOf<Map<String, List<CartridgeSaveBinding.BoundSaveGroup>>>(emptyMap()) }
    var message by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Game?>(null) }
    var importPreview by remember { mutableStateOf<Pair<Uri, CartSaveArchive.Manifest>?>(null) }
    var revision by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<ExportRequest?>(null) }

    LaunchedEffect(games, binding, revision) {
        val result =
            withContext(Dispatchers.IO) { runCatching { games.associate { it.cartridgeId to files(binding, it) } } }
        result.onSuccess { inventory = it }.onFailure { message = "Save inventory could not be read." }
    }

    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { inspectDocument(context, uri) } }
                    result.onSuccess { importPreview = uri to it }
                        .onFailure { message = "The backup is invalid or unreadable." }
                    busy = false
                }
            }
        }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            val request = pendingExport
            pendingExport = null
            if (uri != null && request != null) {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { export(context, binding, uri, request) } }
                    message =
                        result.fold(
                            { if (it) "Backup written and verified." else "Backup written. Readback unavailable." },
                            { "Backup failed. Close games and sync, then check free storage." },
                        )
                    busy = false
                }
            }
        }

    fun requestExport(
        gamesToExport: List<Game>,
        exportScope: CartSaveArchive.Scope,
    ) {
        selected = null
        pendingExport = ExportRequest(gamesToExport, exportScope)
        save.launch("CARTDECK-${System.currentTimeMillis()}.cartsave")
    }

    val rows =
        games.mapNotNull { game ->
            val groups = inventory[game.cartridgeId].orEmpty()
            if (groups.isEmpty()) return@mapNotNull null
            val files = groups.flatMap { it.files }
            val progress =
                files.count {
                    it.kind == CartSaveArchive.Kind.GAME_SAVE || it.kind == CartSaveArchive.Kind.MEMORY_CARD
                }
            val states =
                files.count {
                    it.kind == CartSaveArchive.Kind.MANUAL_STATE || it.kind == CartSaveArchive.Kind.AUTOSAVE
                }
            val bytes = files.sumOf { it.file.length() }
            SaveInventoryRow(
                game.cartridgeId,
                game.title,
                "${game.systemId} · $progress saves or cards · $states states · $bytes bytes",
            )
        }
    SafeSaveSurface(
        playerName = "current local player",
        inventory = rows,
        availableActions =
            buildList {
                if (!busy) {
                    add(
                        ProductAction("Recovery", "Roll back an interrupted restore") {
                            busy = true
                            scope.launch {
                                val result =
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            val lease = checkNotNull(SaveStorageLease.acquire(context))
                                            lease.use {
                                                val pending = BoundSaveRestore.recoveryPending(context)
                                                if (pending) BoundSaveRestore(context).recover()
                                                pending
                                            }
                                        }
                                    }
                                message =
                                    result.fold(
                                        { if (it) "Original saves recovered." else "No interrupted restore." },
                                        { "Recovery could not finish. Original backup copies are retained." },
                                    )
                                revision++
                                busy = false
                            }
                        },
                    )
                    if (rows.isNotEmpty()) {
                        add(
                            ProductAction("Export collection", "Export identified app-managed progress") {
                                requestExport(games, CartSaveArchive.Scope.PLAYER)
                            },
                        )
                    }
                    add(
                        ProductAction("Import backup", "Verify a .cartsave before restoring") {
                            open.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        },
                    )
                }
                add(
                    ProductAction("Coverage", "View the limits of this backup") {
                        message = "Includes bound save RAM, supported PS1 cards and known app-managed states. " +
                            "Native engine storage and unresolved historical saves are excluded."
                    },
                )
            },
        onDetails = { id -> if (!busy) selected = games.firstOrNull { it.cartridgeId == id } },
        modifier = modifier,
    )

    selected?.let { game ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(game.title) },
            text = { Text(rows.firstOrNull { it.id == game.cartridgeId }?.summary.orEmpty()) },
            confirmButton = {
                TextButton(
                    onClick = { requestExport(listOf(game), CartSaveArchive.Scope.GAME) },
                ) { Text("Export game") }
            },
            dismissButton = {
                TextButton(onClick = {
                    requestExport(games.filter { it.systemId == game.systemId }, CartSaveArchive.Scope.CONSOLE)
                }) { Text("Export console") }
            },
        )
    }
    importPreview?.let { (uri, manifest) ->
        val files = manifest.saveGroups.flatMap { it.artifacts }
        val states =
            files.count {
                it.saveKind == CartSaveArchive.Kind.MANUAL_STATE || it.saveKind == CartSaveArchive.Kind.AUTOSAVE
            }
        AlertDialog(
            onDismissRequest = { importPreview = null },
            title = { Text("Verified backup") },
            text = {
                val groupCount = manifest.saveGroups.size
                val bytes = files.sumOf { it.byteLength }
                Text(
                    "$groupCount save groups · $states states · $bytes bytes. " +
                        "SafeSave copies current progress before replacing compatible saves.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importPreview = null
                    busy = true
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                runCatching { restoreDocument(context, binding, games, uri, manifest) }
                            }
                        message =
                            result.fold(
                                { "Backup restored. Open the game to verify progress." },
                                { (it as? RestoreRefused)?.message ?: "Restore failed. Check Recovery." },
                            )
                        revision++
                        busy = false
                    }
                }) { Text("Restore after backup") }
            },
            dismissButton = { TextButton(onClick = { importPreview = null }) { Text("Cancel") } },
        )
    }
    message?.let { info ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("SafeSave") },
            text = { Text(info) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Done") } },
        )
    }
}

private data class ExportRequest(val games: List<Game>, val scope: CartSaveArchive.Scope)

private fun files(
    binding: CartridgeSaveBinding,
    game: Game,
    includeMissing: Boolean = false,
): List<CartridgeSaveBinding.BoundSaveGroup> {
    val system = GameSystem.all().firstOrNull { it.id.dbname == game.systemId }
    return binding.inspectBoundSaves(game, system?.systemCoreConfigs.orEmpty(), includeMissing)
}

private fun inspectDocument(
    context: Context,
    uri: Uri,
): CartSaveArchive.Manifest {
    val temp = File.createTempFile("cartsave-import-", ".zip", context.cacheDir)
    try {
        requireNotNull(context.contentResolver.openInputStream(uri)).use {
            ModFiles.copyBounded(it, temp, CartSaveArchive.MAX_ARCHIVE_BYTES)
        }
        return CartSaveArchive.inspect(temp)
    } finally {
        temp.delete()
    }
}

private fun export(
    context: Context,
    binding: CartridgeSaveBinding,
    uri: Uri,
    request: ExportRequest,
): Boolean {
    val stage = File(context.cacheDir, "cartsave-${UUID.randomUUID()}.zip")
    try {
        val lease = checkNotNull(SaveStorageLease.acquire(context)) { "Save storage is busy" }
        val manifest =
            lease.use {
                check(!BoundSaveRestore.recoveryPending(context))
                val player = GemRatingLocalStore.open(context).session.profileId
                val payloads = mutableMapOf<String, File>()
                val groups =
                    request.games.flatMap { game ->
                        files(binding, game).map { group ->
                            val artifacts =
                                group.files.map { file ->
                                    CartSaveArchive.artifact(file.kind, file.file, file.logicalName, file.slot).also {
                                        payloads[it.artifactId] = file.file
                                    }
                                }
                            CartSaveArchive.Group(
                                UUID.randomUUID().toString(),
                                if (artifacts.any { it.saveKind == CartSaveArchive.Kind.MEMORY_CARD }) {
                                    CartSaveArchive.Scope.CARD_BANK
                                } else {
                                    CartSaveArchive.Scope.GAME
                                },
                                game.systemId,
                                group.engineId,
                                compatibility(group, artifacts),
                                listOf(game.cartridgeId),
                                artifacts,
                                ownerId = player,
                            )
                        }
                    }
                val exclusions =
                    SystemID.entries.map {
                        CartSaveArchive.Exclusion(it.dbname, "Engine-owned or unbound storage is outside this export")
                    }
                val date =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                CartSaveArchive.Manifest(
                    UUID.randomUUID().toString(),
                    date.format(Date()),
                    request.scope,
                    CartSaveArchive.Source(13, CartSaveArchive.Player(player)),
                    CartSaveArchive.Coverage(groups.size, exclusions.size, exclusions),
                    groups,
                ).also { CartSaveArchive.write(stage, it, payloads) }
            }
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { output ->
            stage.inputStream().use { input -> input.copyTo(output) }
        }
        val readback =
            try {
                context.contentResolver.openInputStream(uri)
            } catch (_: java.io.FileNotFoundException) {
                null
            } catch (_: UnsupportedOperationException) {
                null
            } ?: return false
        val temp = File.createTempFile("cartsave-readback-", ".zip", context.cacheDir)
        try {
            readback.use { ModFiles.copyBounded(it, temp, CartSaveArchive.MAX_ARCHIVE_BYTES) }
            check(CartSaveArchive.inspect(temp) == manifest) { "Exported backup manifest differs" }
            return true
        } finally {
            temp.delete()
        }
    } finally {
        stage.delete()
    }
}

private class RestoreRefused(message: String) : IllegalArgumentException(message)

private fun compatibility(
    group: CartridgeSaveBinding.BoundSaveGroup,
    artifacts: List<CartSaveArchive.Artifact>,
): String =
    if (artifacts.all { it.saveKind == CartSaveArchive.Kind.MEMORY_CARD }) {
        "ps1-raw-128k"
    } else {
        "cartdeck-${BuildConfig.VERSION_CODE}/state-protocol-${group.stateVersion}"
    }

private fun restoreDocument(
    context: Context,
    binding: CartridgeSaveBinding,
    games: List<Game>,
    uri: Uri,
    manifest: CartSaveArchive.Manifest,
) {
    val archive = File.createTempFile("cartsave-restore-", ".zip", context.cacheDir)
    try {
        requireNotNull(context.contentResolver.openInputStream(uri)).use {
            ModFiles.copyBounded(it, archive, CartSaveArchive.MAX_ARCHIVE_BYTES)
        }
        val extracted = CartSaveArchive.extractVerified(archive, context.cacheDir.canonicalFile, manifest)
        try {
            val lease =
                SaveStorageLease.acquire(context)
                    ?: throw RestoreRefused("Close the game or wait for save sync to finish.")
            lease.use {
                val owner = GemRatingLocalStore.open(context).session.profileId
                val replacements =
                    manifest.saveGroups.flatMap { group ->
                        if (group.ownerId != owner || manifest.source.player?.id != owner) {
                            throw RestoreRefused("This backup belongs to a different local player.")
                        }
                        val gameId = group.linkedCartridgeIds.singleOrNull()
                        val game =
                            games.singleOrNull { it.cartridgeId == gameId && it.systemId == group.platformId }
                                ?: throw RestoreRefused("A cartridge in this backup has no exact local match.")
                        val destination =
                            files(binding, game, includeMissing = true).singleOrNull {
                                it.engineId == group.engineId &&
                                    compatibility(it, group.artifacts) == group.engineVersion
                            } ?: throw RestoreRefused("No compatible save binding.")
                        val archivedNames = group.artifacts.map { it.relativePath }.toSet()
                        val archivedSlots = group.artifacts.mapNotNull { it.slot }.toSet()
                        if (destination.files.any {
                                it.slot in archivedSlots && it.file.exists() && it.logicalName !in archivedNames
                            }
                        ) {
                            throw RestoreRefused("The backup lacks an existing sidecar for a selected save slot.")
                        }
                        group.artifacts.map { artifact ->
                            val target =
                                destination.files.singleOrNull {
                                    it.logicalName == artifact.relativePath && it.kind == artifact.saveKind &&
                                        it.slot == artifact.slot
                                } ?: throw RestoreRefused("A backup file has no safe destination.")
                            val source = File(extracted, artifact.path)
                            if (artifact.saveKind == CartSaveArchive.Kind.MEMORY_CARD) {
                                if (source.length() != PlayStationMemoryCard.SIZE.toLong() ||
                                    !PlayStationMemoryCard.isRawCard(source.readBytes())
                                ) {
                                    throw RestoreRefused("The PlayStation card image is invalid.")
                                }
                            }
                            if (artifact.saveKind == CartSaveArchive.Kind.MANUAL_STATE ||
                                artifact.saveKind == CartSaveArchive.Kind.AUTOSAVE
                            ) {
                                validateStateSize(source)
                            }
                            BoundSaveRestore.Replacement(target.file, source, artifact)
                        }
                    }
                BoundSaveRestore(context).restore(replacements)
            }
        } finally {
            extracted.deleteRecursively()
        }
    } finally {
        archive.delete()
    }
}

private fun validateStateSize(source: File) {
    val limit = 128L * 1024 * 1024
    if (source.length() !in 1L..limit) throw RestoreRefused("The save state exceeds the import size limit.")
    source.inputStream().buffered().use { input ->
        input.mark(2)
        val gzip = input.read() == 0x1f && input.read() == 0x8b
        input.reset()
        if (gzip) {
            java.util.zip.GZIPInputStream(input).use { stream ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > limit) throw RestoreRefused("The unpacked save state exceeds the import size limit.")
                }
            }
        }
    }
}
