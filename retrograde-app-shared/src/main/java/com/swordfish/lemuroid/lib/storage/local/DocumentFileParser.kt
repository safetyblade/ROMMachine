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

        // A verified signature or a known legacy serial takes precedence.
        // SAF storage sometimes cannot provide a seekable descriptor: allow
        // an explicit console folder as a fallback, never an extension guess.
        // Positive Wii identity must not be overridden by a GameCube folder.
        val system =
            NativeDiscFolderIdentity.resolve(
                baseStorageFile.name,
                baseStorageFile.path,
                GameSystem.all().map { it.id }.toSet(),
                verified = nativeSystem,
                legacy = diskInfo?.systemID,
                foreignDisc = discIdentity?.platform == NextGenDiscIdentity.Platform.WII,
            )
        if (system != null && nativeSystem == null && diskInfo?.systemID == null) {
            Timber.i("Native disc identified from explicit console folder: ${system.dbname}")
        }
        val crc32 =
            if (baseStorageFile.size < MAX_SIZE_CRC32 && diskInfo?.serial == null && system == null) {
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
