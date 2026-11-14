package com.swordfish.lemuroid.app.mods

import com.swordfish.lemuroid.lib.mods.ModArchive
import com.swordfish.lemuroid.lib.mods.ModFiles
import com.swordfish.lemuroid.lib.mods.ModPatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Standalone JVM fixtures: no Android runtime, commercial ROMs or third-party mod assets. */
object ModEngineChecks {
    private var passed = 0

    private fun test(
        name: String,
        block: (File) -> Unit,
    ) {
        val root = Files.createTempDirectory("cartmods-check-").toFile()
        try {
            block(root)
            passed++
            println("PASS $name")
        } finally {
            root.deleteRecursively()
        }
    }

    private fun rejects(block: () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (
            _: IllegalArgumentException,
        ) {
            rejected = true
        } catch (
            _: IllegalStateException,
        ) {
            rejected = true
        } catch (_: java.io.IOException) {
            rejected = true
        }
        check(rejected) { "Unsafe input accepted" }
    }

    private fun zip(
        root: File,
        vararg entries: Pair<String, ByteArray>,
    ): File {
        val result = File(root, "pack.zip")
        ZipOutputStream(result.outputStream()).use { output ->
            entries.forEach { (name, data) ->
                output.putNextEntry(ZipEntry(name))
                output.write(data)
                output.closeEntry()
            }
        }
        return result
    }

    private fun ips(vararg data: Int): ByteArray =
        "PATCH".toByteArray() +
            data.map {
                it.toByte()
            }.toByteArray() + "EOF".toByteArray()

    private fun crc(data: ByteArray): Long = CRC32().apply { update(data) }.value

    private fun ByteArrayOutputStream.le32(value: Long) {
        repeat(4) { write((value ushr (8 * it)).toInt() and 255) }
    }

    private fun ByteArrayOutputStream.number(value: Long) {
        var remaining = value
        while (true) {
            val digit = (remaining and 127).toInt()
            remaining = remaining ushr 7
            if (remaining == 0L) {
                write(digit or 128)
                return
            }
            write(digit)
            remaining--
        }
    }

    private fun bps(
        source: ByteArray,
        target: ByteArray,
        commands: ByteArrayOutputStream.() -> Unit,
    ): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write("BPS1".toByteArray())
        bytes.number(source.size.toLong())
        bytes.number(target.size.toLong())
        bytes.number(0)
        bytes.commands()
        bytes.le32(crc(source))
        bytes.le32(crc(target))
        bytes.le32(crc(bytes.toByteArray()))
        return bytes.toByteArray()
    }

    private fun patch(
        root: File,
        source: ByteArray,
        target: ByteArray,
        bytes: ByteArray,
    ) {
        val base = File(root, "base.rom").apply { writeBytes(source) }
        val patch = File(root, "mod.patch").apply { writeBytes(bytes) }
        val expected = File(root, "expected").apply { writeBytes(target) }
        val output = File(root, "edition.rom")
        ModPatch().apply(base, patch, output, ModFiles.sha256(base), ModFiles.sha256(expected))
        check(output.readBytes().contentEquals(target))
        check(base.readBytes().contentEquals(source))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        passed = 0
        test("valid ZIP extraction and CRC verification") { root ->
            val archive = zip(root, "textures.ini" to "[hashes]".toByteArray(), "art/a.png" to byteArrayOf(1, 2))
            val stage = ModArchive().extract(archive, File(root, "staging"), { true })
            check(File(stage, "art/a.png").readBytes().contentEquals(byteArrayOf(1, 2)))
        }
        listOf("../escape", "/absolute", "a/../../escape", "C:/drive", "a\\b", "a//b", "a/./b").forEach { path ->
            test("reject path $path before extraction") { root ->
                val staging = File(root, "staging")
                rejects { ModArchive().extract(zip(root, path to byteArrayOf(1)), staging, { true }) }
                check(!staging.exists())
            }
        }
        test("reject duplicate case-folded paths") { root ->
            rejects { ModArchive().inspect(zip(root, "a.PNG" to byteArrayOf(1), "a.png" to byteArrayOf(2))) }
        }
        test("reject parent file collision") { root ->
            rejects { ModArchive().inspect(zip(root, "art" to byteArrayOf(1), "art/a.png" to byteArrayOf(2))) }
        }
        test("reject symlink Unix metadata") { root ->
            val archive = zip(root, "link" to "../outside".toByteArray())
            val bytes = archive.readBytes()
            val central =
                bytes.indices.first {
                    it + 3 < bytes.size && bytes[it] == 0x50.toByte() &&
                        bytes[it + 1] == 0x4b.toByte() && bytes[it + 2] == 1.toByte() &&
                        bytes[it + 3] == 2.toByte()
                }
            bytes[central + 5] = 3
            bytes[central + 41] = 0xa0.toByte()
            archive.writeBytes(bytes)
            rejects { ModArchive().inspect(archive) }
        }
        test("reject mismatched local filename") { root ->
            val archive = zip(root, "safe" to byteArrayOf(1))
            val bytes = archive.readBytes()
            bytes[30] = 'X'.code.toByte()
            archive.writeBytes(bytes)
            rejects { ModArchive().inspect(archive) }
        }
        test("reject unsupported extension before writing") { root ->
            val staging = File(root, "staging")
            rejects { ModArchive().extract(zip(root, "run.exe" to byteArrayOf(1)), staging, { it.endsWith(".png") }) }
            check(!staging.exists())
        }
        test("reject ZIP expansion budget") { root ->
            rejects { ModArchive(ModArchive.Limits(expandedBytes = 10)).inspect(zip(root, "a" to ByteArray(11))) }
        }
        test("reject ZIP file count budget") { root ->
            rejects {
                ModArchive(
                    ModArchive.Limits(entries = 1),
                ).inspect(zip(root, "a" to byteArrayOf(1), "b" to byteArrayOf(2)))
            }
        }
        test("reject ZIP compression bomb") { root ->
            rejects { ModArchive().inspect(zip(root, "a" to ByteArray(1024 * 1024))) }
        }
        test("reject incomplete ZIP") { root ->
            val archive = zip(root, "a" to byteArrayOf(1))
            archive.writeBytes(archive.readBytes().dropLast(5).toByteArray())
            rejects { ModArchive().inspect(archive) }
        }
        test("cancelled extraction removes partial storage") { root ->
            val staging = File(root, "staging")
            rejects { ModArchive().extract(zip(root, "a" to byteArrayOf(1)), staging, { true }, { true }) }
            check(staging.listFiles()!!.isEmpty())
        }
        test("insufficient storage leaves no staging") { root ->
            val staging = File(root, "staging")
            rejects {
                ModArchive(
                    ModArchive.Limits(reserveBytes = Long.MAX_VALUE),
                ).extract(zip(root, "a" to byteArrayOf(1)), staging, {
                    true
                })
            }
            check(staging.listFiles()!!.isEmpty())
        }
        test(
            "IPS literal preserves source",
        ) { root -> patch(root, byteArrayOf(1, 2, 3), byteArrayOf(1, 9, 3), ips(0, 0, 1, 0, 1, 9)) }
        test(
            "IPS RLE and expansion",
        ) { root -> patch(root, byteArrayOf(1), byteArrayOf(1, 7, 7, 7), ips(0, 0, 1, 0, 0, 0, 3, 7)) }
        test(
            "IPS truncate extension",
        ) { root -> patch(root, byteArrayOf(1, 2, 3), byteArrayOf(1), ips() + byteArrayOf(0, 0, 1)) }
        test("BPS source read and literal") { root ->
            val source = "abc".toByteArray()
            val target = "abZ".toByteArray()
            patch(
                root,
                source,
                target,
                bps(source, target) {
                    number(4)
                    number(1)
                    write('Z'.code)
                },
            )
        }
        test("BPS source relative copy") { root ->
            val source = "abcdef".toByteArray()
            val target = "defabc".toByteArray()
            patch(
                root,
                source,
                target,
                bps(source, target) {
                    number(10)
                    number(6)
                    number(10)
                    number(13)
                },
            )
        }
        test("BPS overlapping target copy") { root ->
            val source = "x".toByteArray()
            val target = "abcabcabc".toByteArray()
            patch(
                root,
                source,
                target,
                bps(source, target) {
                    number(9)
                    write("abc".toByteArray())
                    number(23)
                    number(0)
                },
            )
        }
        test("BPS long varints") { root ->
            val source = ByteArray(20000) { (it % 251).toByte() }
            patch(root, source, source, bps(source, source) { number((source.size.toLong() - 1) shl 2) })
        }
        test("wrong base checksum never commits") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val patch = File(root, "patch").apply { writeBytes(ips()) }
            val output = File(root, "output")
            rejects { ModPatch().apply(source, patch, output, "0".repeat(64), ModFiles.sha256(source)) }
            check(!output.exists() && source.readText() == "base")
        }
        test("wrong target checksum cleans stage") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val patch = File(root, "patch").apply { writeBytes(ips()) }
            val output = File(root, "output")
            rejects { ModPatch().apply(source, patch, output, ModFiles.sha256(source), "0".repeat(64)) }
            check(!output.exists() && root.listFiles()!!.none { it.name.startsWith(".patch-") })
        }
        test("corrupt BPS patch CRC rejects target") { root ->
            val bytes = "abc".toByteArray()
            val source = File(root, "base").apply { writeBytes(bytes) }
            val input =
                File(root, "patch").apply {
                    writeBytes(
                        bps(bytes, bytes) { number(8) }.also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() },
                    )
                }
            val output = File(root, "output")
            rejects { ModPatch().apply(source, input, output, ModFiles.sha256(source), ModFiles.sha256(source)) }
            check(!output.exists())
        }
        test("output cap rejects excessive IPS allocation") { root ->
            val source = File(root, "base").apply { writeText("a") }
            val input = File(root, "patch").apply { writeBytes(ips(0, 1, 0, 0, 1, 9)) }
            rejects {
                ModPatch(
                    10,
                ).apply(source, input, File(root, "output"), ModFiles.sha256(source), ModFiles.sha256(source))
            }
        }
        test("unsupported patch fails without edition") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val input = File(root, "patch").apply { writeText("VCDIFF unsupported") }
            rejects {
                ModPatch().apply(
                    source,
                    input,
                    File(root, "output"),
                    ModFiles.sha256(source),
                    ModFiles.sha256(source),
                )
            }
            check(!File(root, "output").exists())
        }
        test("cancelled patch never commits") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val input = File(root, "patch").apply { writeBytes(ips()) }
            rejects {
                ModPatch().apply(
                    source,
                    input,
                    File(root, "output"),
                    ModFiles.sha256(source),
                    ModFiles.sha256(source),
                    { true },
                )
            }
            check(!File(root, "output").exists())
        }
        test("corrupt stored ZIP data cleans extracted stage") { root ->
            val archive = File(root, "bad.zip")
            val data = byteArrayOf(1, 2, 3)
            ZipOutputStream(archive.outputStream()).use { output ->
                val entry =
                    ZipEntry("a.png").apply {
                        method = ZipEntry.STORED
                        size = data.size.toLong()
                        compressedSize = size
                        crc = crc(data)
                    }
                output.putNextEntry(entry)
                output.write(data)
                output.closeEntry()
            }
            val bytes = archive.readBytes()
            bytes[35] = 9
            archive.writeBytes(bytes)
            val staging = File(root, "staging")
            rejects { ModArchive().extract(archive, staging, { true }) }
            check(staging.listFiles()!!.isEmpty())
        }
        test("existing edition is never replaced") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val input = File(root, "patch").apply { writeBytes(ips()) }
            val output = File(root, "output").apply { writeText("progress sentinel") }
            rejects { ModPatch().apply(source, input, output, ModFiles.sha256(source), ModFiles.sha256(source)) }
            check(output.readText() == "progress sentinel")
        }
        test("truncated IPS cleans staging and retains source") { root ->
            val source = File(root, "base").apply { writeText("base") }
            val input = File(root, "patch").apply { writeText("PATCH") }
            rejects {
                ModPatch().apply(
                    source,
                    input,
                    File(root, "output"),
                    ModFiles.sha256(source),
                    ModFiles.sha256(source),
                )
            }
            check(source.readText() == "base" && root.listFiles()!!.none { it.name.startsWith(".patch-") })
        }
        test("BPS cannot copy unwritten target bytes") { root ->
            val bytes = byteArrayOf(1)
            val source = File(root, "base").apply { writeBytes(bytes) }
            val input =
                File(root, "patch").apply {
                    writeBytes(
                        bps(bytes, bytes) {
                            number(3)
                            number(0)
                        },
                    )
                }
            rejects {
                ModPatch().apply(
                    source,
                    input,
                    File(root, "output"),
                    ModFiles.sha256(source),
                    ModFiles.sha256(source),
                )
            }
            check(!File(root, "output").exists())
        }
        test("SHA verification responds to cancellation") { root ->
            val source = File(root, "base").apply { writeBytes(ByteArray(1024)) }
            rejects { ModFiles.sha256(source) { true } }
        }
        println("$passed CartMods engine checks passed")
    }
}
