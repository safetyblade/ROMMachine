package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import com.swordfish.lemuroid.common.kotlin.calculateCrc32
import com.swordfish.lemuroid.common.kotlin.toStringCRC32
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.lib.storage.scanner.NativeDiscFolderIdentity
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import com.swordfish.lemuroid.lib.storage.scanner.SerialScanner
import timber.log.Timber
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object DocumentFileParser {
    private const val MAX_CHECKED_ENTRIES = 3
    private const val SINGLE_ARCHIVE_THRESHOLD = 0.9
    private const val MAX_SIZE_CRC32 = 1_000_000_000

    // Full-file CRC has no useful scanner identity for these native disc
    // containers. A 0.95 GB CISO or ambiguous ISO could otherwise consume
    // hundreds of megabytes of SAF reads and make refresh appear to hang.
    private val NO_FULL_FILE_CRC_DISC_EXTENSIONS =
        setOf("iso", "ciso", "chd", "cdi", "gdi", "gcm", "rvz", "gcz", "cso", "isz", "tgc")

    fun parseDocumentFile(
        context: Context,
        baseStorageFile: BaseStorageFile,
    ): StorageFile {
        return if (baseStorageFile.extension == "zip") {
            Timber.d("Detected zip file. ${baseStorageFile.name}")
            parseZipFile(context, baseStorageFile)
        } else {
            Timber.d("Detected standard file. ${baseStorageFile.name}")
            parseStandardFile(context, baseStorageFile)
        }
    }

    private fun parseZipFile(
        context: Context,
        baseStorageFile: BaseStorageFile,
    ): StorageFile {
        val inputStream = context.contentResolver.openInputStream(baseStorageFile.uri)
        return ZipInputStream(inputStream).use {
            val gameEntry = findGameEntry(it, baseStorageFile.size)
            if (gameEntry != null) {
                Timber.d("Handing zip file as compressed game: ${baseStorageFile.name}")
                parseCompressedGame(baseStorageFile, gameEntry, it)
            } else {
                Timber.d("Handing zip file as standard: ${baseStorageFile.name}")
                parseStandardFile(context, baseStorageFile)
            }
        }
    }

    private fun parseCompressedGame(
        baseStorageFile: BaseStorageFile,
        entry: ZipEntry,
        zipInputStream: ZipInputStream,
    ): StorageFile {
        Timber.d("Processing zipped entry: ${entry.name}")

        val diskInfo = SerialScanner.extractInfo(entry.name, zipInputStream)

        return StorageFile(
            entry.name,
            entry.size,
            entry.crc.toStringCRC32(),
            diskInfo.serial,
            baseStorageFile.uri,
            baseStorageFile.uri.path,
            diskInfo.systemID,
        )
    }

    private fun parseStandardFile(
        context: Context,
        baseStorageFile: BaseStorageFile,
    ): StorageFile {
        val discIdentity = identifyNativeDisc(context, baseStorageFile)
        val nativeSystem =
            when (discIdentity?.platform) {
                NextGenDiscIdentity.Platform.DREAMCAST -> SystemID.DREAMCAST
                NextGenDiscIdentity.Platform.GAMECUBE -> SystemID.GAMECUBE
                NextGenDiscIdentity.Platform.PS2 -> SystemID.PLAYSTATION_2
                else -> null
            }
        val diskInfo =
            context.contentResolver.openInputStream(baseStorageFile.uri)
                ?.let { inputStream -> SerialScanner.extractInfo(baseStorageFile.name, inputStream) }

        // Positive signatures and old PS1/PSP serials always take priority.
        // GDI/CDI/GCM are uniquely assigned to registered native consoles;
        // shared ISO/CHD and compressed disc formats require a clear folder.
        // Wii magic must never become GameCube/PS2 just because of a folder.
        val uniqueNativeSystem =
            if (discIdentity?.platform == null && diskInfo?.systemID == null) {
                GameSystem.findByUniqueFileExtension(baseStorageFile.extension)
                    ?.id?.takeIf {
                        it == SystemID.DREAMCAST ||
                            it == SystemID.GAMECUBE ||
                            it == SystemID.PLAYSTATION_2
                    }
            } else {
                null
            }
        val system =
            NativeDiscFolderIdentity.resolve(
                baseStorageFile.name,
                baseStorageFile.path,
                GameSystem.all().map { it.id }.toSet(),
                verified = nativeSystem ?: uniqueNativeSystem,
                legacy = diskInfo?.systemID,
                foreignDisc = discIdentity?.platform == NextGenDiscIdentity.Platform.WII,
            )
        if (system != null && nativeSystem == null && diskInfo?.systemID == null) {
            Timber.i("Native disc identified from explicit console folder: ${system.dbname}")
        }
        val crc32 =
            if (baseStorageFile.size < MAX_SIZE_CRC32 &&
                diskInfo?.serial == null &&
                system == null &&
                baseStorageFile.extension.lowercase(java.util.Locale.US) !in NO_FULL_FILE_CRC_DISC_EXTENSIONS
            ) {
                context.contentResolver.openInputStream(baseStorageFile.uri)?.calculateCrc32()
            } else {
                null
            }

        Timber.d("Parsed standard file: $baseStorageFile")

        return StorageFile(
            baseStorageFile.name,
            baseStorageFile.size,
            crc32,
            diskInfo?.serial,
            baseStorageFile.uri,
            baseStorageFile.path,
            system,
        )
    }

    private fun identifyNativeDisc(
        context: Context,
        file: BaseStorageFile,
    ): NextGenDiscIdentity.Result? {
        return runCatching {
            when (file.extension.lowercase(java.util.Locale.US)) {
                "gdi" ->
                    context.contentResolver.openInputStream(file.uri)?.use {
                        val text = it.takeIf { file.size in 1..65536 }?.bufferedReader()?.readText()
                        NextGenDiscIdentity.probe(file.name, gdiText = text)
                    }
                // Unlike a shared .iso suffix, a real Nintendo CISO's mapped
                // first block identifies GameCube/Wii without folder guesses.
                "ciso" ->
                    context.contentResolver.openInputStream(file.uri)?.use {
                        NextGenDiscIdentity.probeNintendoCiso(it)
                    }
                "iso", "gcm", "chd" ->
                    context.contentResolver.openFileDescriptor(file.uri, "r")?.use { descriptor ->
                        FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                            if (file.extension.equals("chd", ignoreCase = true)) {
                                NextGenDiscIdentity.probeDreamcastChd(channel)
                            } else {
                                NextGenDiscIdentity.probePs2Iso(
                                    if (file.extension.equals(
                                            "gcm",
                                            ignoreCase = true,
                                        )
                                    ) {
                                        file.name.dropLast(3) + "iso"
                                    } else {
                                        file.name
                                    },
                                    channel,
                                )
                            }
                        }
                    }
                else -> null
            }
        }.getOrNull()
    }

    /* Finds a zip entry which we assume is a game. Lemuroid only supports single archive games,
       so we are looking for an entry which occupies a large percentage of the archive space.
       This is very fast heuristic to compute and avoids reading the whole stream in most
       scenarios.*/
    fun findGameEntry(
        openedInputStream: ZipInputStream,
        fileSize: Long = -1,
    ): ZipEntry? {
        for (i in 0..MAX_CHECKED_ENTRIES) {
            val entry = openedInputStream.nextEntry ?: break
            if (!isGameEntry(entry, fileSize)) continue
            return entry
        }
        return null
    }

    private fun isGameEntry(
        entry: ZipEntry,
        fileSize: Long,
    ): Boolean {
        if (fileSize <= 0 || entry.compressedSize <= 0) return false
        return (entry.compressedSize.toFloat() / fileSize.toFloat()) > SINGLE_ARCHIVE_THRESHOLD
    }
}
