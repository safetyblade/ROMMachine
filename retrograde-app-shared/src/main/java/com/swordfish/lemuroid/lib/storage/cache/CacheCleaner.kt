package com.swordfish.lemuroid.lib.storage.cache

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.swordfish.lemuroid.common.kotlin.gigaBytes
import com.swordfish.lemuroid.common.kotlin.megaBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToLong

object CacheCleaner {
    private val MIN_CACHE_LIMIT = 64L.megaBytes()
    private val MAX_CACHE_LIMIT = 10L.gigaBytes()

    fun getSupportedCacheLimits(): List<Long> =
        listOf(0L) + generateSequence(MIN_CACHE_LIMIT) { it * 2L }
            .takeWhile { it <= MAX_CACHE_LIMIT }.toList()

    fun getDefaultCacheLimit(): Long {
        val defaultCacheSize = (getInternalMemorySize() * 0.01f).roundToLong()
        return getSupportedCacheLimits().filter { it > 0 }.minByOrNull {
            abs(it - minOf(defaultCacheSize, 1L.gigaBytes()))
        } ?: MIN_CACHE_LIMIT
    }

    private fun getInternalMemorySize(): Long {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.absolutePath)
        return stat.blockSizeLong * stat.blockCountLong
    }

    /** Clear only reconstructible media; leave every save, shader and installed game intact. */
    suspend fun cleanAll(appContext: Context) = clean(appContext, 0L)

    suspend fun clean(
        appContext: Context,
        requestedLimit: Long,
    ): Unit =
        withContext(Dispatchers.IO) {
            val coroutine = currentCoroutineContext()
            val limit = getSupportedCacheLimits().minByOrNull { abs(it - requestedLimit) } ?: 0L
            val result = RomCachePruner.prune(appContext.cacheDir, appContext.filesDir, limit) {
                coroutine.ensureActive()
            }
            Timber.i(
                "Staged ROM bytes: ${result.beforeBytes} -> ${result.remainingBytes}; " +
                    "removed ${result.removedGroups} groups",
            )
        }
}
