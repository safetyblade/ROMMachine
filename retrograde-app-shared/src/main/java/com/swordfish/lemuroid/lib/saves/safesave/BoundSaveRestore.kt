package com.swordfish.lemuroid.lib.saves.safesave

import android.content.Context
import android.util.AtomicFile
import com.swordfish.lemuroid.lib.mods.ModFiles
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID

/** The caller resolves destinations through the existing cartridge bindings and holds the storage lease. */
class BoundSaveRestore(context: Context) {
    data class Replacement(val destination: File, val source: File, val artifact: CartSaveArchive.Artifact)

    @Serializable
    private data class Entry(
        val destination: String,
        val priorHash: String?,
        val priorSize: Long,
        val replacementHash: String,
    )

    private val json = Json
    private val entriesSerializer = ListSerializer(Entry.serializer())
    private val directory = File(context.noBackupFilesDir, "safesave/restore")
    private val pending = File(directory, "pending")
    private val journal = AtomicFile(File(pending, "journal.json"))
    private val stores = DirectoriesManager(context)
    private val filesDir = context.filesDir

    fun restore(replacements: List<Replacement>) {
        require(replacements.isNotEmpty())
        require(replacements.map { it.destination }.distinct().size == replacements.size)
        check(!pending.exists()) { "Recover the interrupted restore before replacing more saves" }
        check(pending.mkdirs()) { "SafeSave could not create recovery storage" }
        try {
            val originals = replacements.sumOf { it.destination.length() }
            val largestStage = replacements.maxOf { it.artifact.byteLength }
            check(pending.usableSpace > originals + largestStage) { "Not enough room to back up and stage the saves" }
            val entries =
                replacements.mapIndexed { index, replacement ->
                    val target = target(replacement.destination.path)
                    if (Armsx2FileCards.isPrivateCardPath(filesDir, target)) {
                        require(replacement.artifact.saveKind == CartSaveArchive.Kind.MEMORY_CARD)
                        require(Armsx2FileCards.isFormattedCard(replacement.source))
                    }
                    val priorHash = if (target.exists()) ModFiles.sha256(target) else null
                    val priorSize = target.length()
                    require(replacement.source.length() == replacement.artifact.byteLength)
                    require(ModFiles.sha256(replacement.source) == replacement.artifact.sha256)
                    if (priorHash != null) {
                        copyVerified(target, original(index), priorSize, priorHash)
                    }
                    Entry(target.path, priorHash, priorSize, replacement.artifact.sha256)
                }
            val data = json.encodeToString(entriesSerializer, entries).encodeToByteArray()
            require(data.size <= JOURNAL_LIMIT)
            val stream = journal.startWrite()
            try {
                stream.write(data)
                journal.finishWrite(stream)
            } catch (failure: Throwable) {
                journal.failWrite(stream)
                throw failure
            }
            check(readJournal() == entries)
            replacements.zip(entries).forEach { (replacement, entry) ->
                activate(
                    replacement.source,
                    target(entry.destination),
                    replacement.artifact.byteLength,
                    entry.replacementHash,
                )
            }
            // An atomic directory rename marks the commit and retains the verified originals.
            check(pending.renameTo(File(directory, "previous-${UUID.randomUUID()}"))) { "Could not commit restore" }
        } catch (failure: Throwable) {
            try {
                recover()
            } catch (recovery: Throwable) {
                failure.addSuppressed(recovery)
                throw IOException("Recovery is required before gameplay. Original copies have been retained.", failure)
            }
            throw failure
        }
    }

    /** Roll back any partly applied transaction before an emulator can reopen the affected storage. */
    fun recover() {
        if (!pending.exists()) return
        val base = journal.baseFile
        if (!base.exists() && !File(base.path + ".bak").exists()) {
            // No live files are changed before the durable journal is committed.
            check(pending.deleteRecursively())
            return
        }
        val entries = readJournal()
        val targets = entries.map { target(it.destination) }
        require(targets.distinct().size == targets.size)
        entries.forEachIndexed { index, entry ->
            val current = targets[index].takeIf(File::isFile)?.let { ModFiles.sha256(it) }
            check(current == entry.priorHash || current == entry.replacementHash) {
                "A save changed outside SafeSave. Recovery copies were retained."
            }
            entry.priorHash?.let {
                require(original(index).length() == entry.priorSize && ModFiles.sha256(original(index)) == it)
            }
        }
        entries.forEachIndexed { index, entry ->
            val destination = targets[index]
            if (entry.priorHash == null) {
                if (destination.exists()) check(destination.delete())
            } else {
                activate(original(index), destination, entry.priorSize, entry.priorHash)
            }
        }
        check(pending.renameTo(File(directory, "recovered-${UUID.randomUUID()}")))
    }

    private fun readJournal(): List<Entry> =
        journal.openRead().use {
            require(journal.baseFile.length() in 1..JOURNAL_LIMIT.toLong())
            json.decodeFromString(entriesSerializer, it.reader(Charsets.UTF_8).readText())
        }

    private fun original(index: Int): File = File(pending, "original-$index")

    private fun target(path: String): File {
        val file = File(path)
        val roots = listOf(stores.getSavesDirectory(), stores.getStatesDirectory(), stores.getStatesPreviewDirectory())
        require(file.isAbsolute && file == file.canonicalFile)
        val managed = roots.any { file.path.startsWith(File(it.canonicalFile, "cartridges").path + File.separator) }
        require(managed || Armsx2FileCards.isPrivateCardPath(filesDir, file))
        require(file.parentFile?.isDirectory == true && (!file.exists() || file.isFile))
        return file
    }

    private fun activate(
        source: File,
        destination: File,
        length: Long,
        hash: String,
    ) {
        val stage = File.createTempFile("safesave-", ".pending", destination.parentFile)
        try {
            copyVerified(source, stage, length, hash)
            check(stage.renameTo(destination)) { "SafeSave could not replace the game file" }
            check(destination.length() == length && ModFiles.sha256(destination) == hash)
        } finally {
            stage.delete()
        }
    }

    private fun copyVerified(
        source: File,
        destination: File,
        length: Long,
        hash: String,
    ) {
        require(source.canonicalFile == source && source.isFile)
        source.inputStream().use { require(ModFiles.copyBounded(it, destination, length) == length) }
        require(ModFiles.sha256(destination) == hash)
    }

    companion object {
        private const val JOURNAL_LIMIT = 2 * 1024 * 1024

        fun recoveryPending(context: Context): Boolean =
            File(context.noBackupFilesDir, "safesave/restore/pending").exists()
    }
}
