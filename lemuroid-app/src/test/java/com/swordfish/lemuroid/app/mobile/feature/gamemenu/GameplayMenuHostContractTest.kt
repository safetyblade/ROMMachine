package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayMenuHostContractTest {
    private class FakeHost(
        override val capabilities: GameplayMenuHostCapabilities,
        var pauseResult: GameplayMenuCommandResult = GameplayMenuCommandResult.Completed,
        var resumeResult: GameplayMenuCommandResult = GameplayMenuCommandResult.Completed,
    ) : GameplayMenuCommandHost {
        val commands = mutableListOf<GameplayMenuCommand>()

        override suspend fun execute(command: GameplayMenuCommand): GameplayMenuCommandResult {
            commands.add(command)
            return when (command) {
                GameplayMenuCommand.PAUSE -> pauseResult
                GameplayMenuCommand.RESUME -> resumeResult
                else -> GameplayMenuCommandResult.Completed
            }
        }
    }

    @Test
    fun pauseRequiresAcknowledgementAndIsNotRepeated() =
        runBlocking {
            val host = FakeHost(GameplayMenuHostCapabilities(canPause = true))
            val session = GameplayMenuSession(host)
            assertEquals(GameplayMenuSessionStatus.PAUSED, session.open().status)
            session.open()
            assertEquals(listOf(GameplayMenuCommand.PAUSE), host.commands)
            assertEquals(GameplayMenuSessionStatus.CLOSED, session.resume().status)
            assertEquals(
                listOf(GameplayMenuCommand.PAUSE, GameplayMenuCommand.RESUME),
                host.commands,
            )
        }

    @Test
    fun unsupportedPauseKeepsMenuOpenWithoutClaimingPause() =
        runBlocking {
            val host = FakeHost(GameplayMenuHostCapabilities())
            val session = GameplayMenuSession(host)
            assertEquals(GameplayMenuSessionStatus.OPEN_RUNNING, session.open().status)
            assertTrue(host.commands.isEmpty())
            assertEquals(GameplayMenuSessionStatus.CLOSED, session.resume().status)
            assertTrue(host.commands.isEmpty())
        }

    @Test
    fun failedPauseNeverBecomesPaused() =
        runBlocking {
            val host =
                FakeHost(
                    GameplayMenuHostCapabilities(canPause = true),
                    pauseResult = GameplayMenuCommandResult.Failed("SDL not ready"),
                )
            val session = GameplayMenuSession(host)
            assertEquals(GameplayMenuSessionStatus.OPEN_RUNNING, session.open().status)
            assertTrue(session.snapshot.message.orEmpty().contains("SDL not ready"))
        }

    @Test
    fun failedResumeDoesNotFalselyCloseMenu() =
        runBlocking {
            val host =
                FakeHost(
                    GameplayMenuHostCapabilities(canPause = true),
                    resumeResult = GameplayMenuCommandResult.Failed("thread unavailable"),
                )
            val session = GameplayMenuSession(host)
            session.open()
            assertEquals(GameplayMenuSessionStatus.ERROR, session.resume().status)
            assertTrue(session.snapshot.message.orEmpty().contains("thread unavailable"))
            host.resumeResult = GameplayMenuCommandResult.Completed
            assertEquals(GameplayMenuSessionStatus.CLOSED, session.resume().status)
            assertEquals(
                listOf(GameplayMenuCommand.PAUSE, GameplayMenuCommand.RESUME, GameplayMenuCommand.RESUME),
                host.commands,
            )
        }

    @Test
    fun saveStateIsRejectedWithoutHostCapability() =
        runBlocking {
            val host =
                FakeHost(
                    GameplayMenuHostCapabilities(saveMethodDescription = "Save inside the game."),
                )
            val session = GameplayMenuSession(host)
            session.open()
            assertEquals(
                GameplayMenuCommandResult.Unsupported("Save inside the game."),
                session.request(GameplayMenuCommand.SAVE_STATE),
            )
            assertTrue(host.commands.isEmpty())
        }

    @Test
    fun saveStateRequiresHostCompletion() =
        runBlocking {
            val host = FakeHost(GameplayMenuHostCapabilities(canSaveState = true))
            val session = GameplayMenuSession(host)
            session.open()
            assertEquals(
                GameplayMenuCommandResult.Completed,
                session.request(GameplayMenuCommand.SAVE_STATE),
            )
            assertEquals(listOf(GameplayMenuCommand.SAVE_STATE), host.commands)
        }
}
