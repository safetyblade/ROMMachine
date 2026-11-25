package com.swordfish.lemuroid.app.safesave

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.mods.ModFiles
import com.swordfish.lemuroid.lib.saves.safesave.Armsx2FileCards
import com.swordfish.lemuroid.lib.saves.safesave.BoundSaveRestore
import com.swordfish.lemuroid.lib.saves.safesave.CartSaveArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class Armsx2FileCardsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cartridge = UUID.randomUUID().toString()
    private val root = File(context.filesDir.canonicalFile, "native/ps2/armsx2/cartridges/$cartridge")
    private val cards = File(root, "memcards").apply { mkdirs() }

    @Test fun completeEccCardRoundTripsThroughArchiveAndProtectsTheDisplacedImage() {
        val card = File(cards, "Mcd001.ps2").also { writeFixtureCard(it, 0x12) }
        assertEquals(listOf(card), Armsx2FileCards.discover(context.filesDir, cartridge))
        val artifact = CartSaveArchive.artifact(CartSaveArchive.Kind.MEMORY_CARD, card, card.name)
        val player = UUID.randomUUID().toString()
        val scope = CartSaveArchive.Scope.CARD_BANK
        val manifest =
            CartSaveArchive.Manifest(
                UUID.randomUUID().toString(),
                "2026-11-23T00:00:00Z",
                scope,
                CartSaveArchive.Source(13, CartSaveArchive.Player(player)),
                CartSaveArchive.Coverage(1, 0, emptyList()),
                listOf(
                    CartSaveArchive.Group(
                        UUID.randomUUID().toString(),
                        scope,
                        "ps2",
                        Armsx2FileCards.ENGINE_ID,
                        Armsx2FileCards.FORMAT,
                        listOf(cartridge),
                        listOf(artifact),
                        ownerId = player,
                    ),
                ),
            )
        val archive = File(context.cacheDir.canonicalFile, "whole-ps2-card.cartsave")
        CartSaveArchive.write(archive, manifest, mapOf(artifact.artifactId to card))
        val staging = File(context.cacheDir.canonicalFile, "card-restore").apply { mkdirs() }
        val extracted = CartSaveArchive.extractVerified(archive, staging, manifest)
        writeFixtureCard(card, 0x34)
        val originalHash = ModFiles.sha256(card)
        BoundSaveRestore(
            context,
        ).restore(listOf(BoundSaveRestore.Replacement(card, File(extracted, artifact.path), artifact)))

        assertEquals(8L * 1024 * 528 * 2, card.length())
        assertEquals(artifact.sha256, ModFiles.sha256(card))
        val history = File(context.noBackupFilesDir, "safesave/restore").listFiles().orEmpty()
        assertEquals(
            originalHash,
            ModFiles.sha256(File(history.single { it.name.startsWith("previous-") }, "original-0")),
        )
        assertFalse(BoundSaveRestore.recoveryPending(context))
    }

    @Test fun refusesMixedOrUnrecognizedStorageAndCustomSlotPaths() {
        val card = File(cards, "Mcd001.ps2").also { writeFixtureCard(it, 0x12) }
        val unformatted = File(cards, "Mcd002.ps2").apply { writeText("Unrelated bytes") }
        val other = File(cards, "Pending BIOS.bin").also { writeFixtureCard(it, 0x34) }
        assertEquals(listOf(card), Armsx2FileCards.discover(context.filesDir, cartridge))
        File(root, "PCSX2-Android.ini").writeText("[MemoryCards]\nSlot1_Filename = swapped.ps2\n")
        assertEquals(emptyList<File>(), Armsx2FileCards.discover(context.filesDir, cartridge, includeMissing = true))
        val artifact = CartSaveArchive.artifact(CartSaveArchive.Kind.MEMORY_CARD, card)
        assertThrows(IllegalArgumentException::class.java) {
            BoundSaveRestore(context).restore(listOf(BoundSaveRestore.Replacement(other, card, artifact)))
        }
        assertEquals("Unrelated bytes", unformatted.readText())
    }

    private fun writeFixtureCard(
        card: File,
        lastByte: Int,
    ) {
        RandomAccessFile(card, "rw").use { file ->
            file.setLength(8L * 1024 * 528 * 2)
            file.writeBytes("Sony PS2 Memory Card Format ")
            file.seek(file.length() - 1)
            file.write(lastByte)
        }
    }
}
