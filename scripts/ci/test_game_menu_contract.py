"""Source integration guardrails alongside executable Kotlin/Compose menu tests."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAVA = ROOT / 'lemuroid-app/src/main/java'
MENU = JAVA / 'com/swordfish/lemuroid/app/mobile/feature/gamemenu'


class GameMenuIntegrationTest(unittest.TestCase):
    def test_native_menu_resume_never_transfers_or_relaunches_the_guest(self):
        source = (JAVA / 'com/rfandango/haku_x/NativeCoreActivity.kt').read_text()
        actions = source.split('private fun showMenu()')[1].split('private fun rollbackEngine()')[0]
        self.assertIn('menuOpen = true', actions)
        self.assertIn('menuOpen = false', actions)
        self.assertIn('hardware.clear()', actions)
        self.assertIn('sendHardware()', actions)
        for forbidden in ('startActivity(', 'AlertDialog', 'loadLibraries(', 'nativeSetPads(', 'finish('):
            self.assertNotIn(forbidden, actions)
        self.assertEqual(1, source.count('nativeSetPaused(it)'))
        self.assertNotIn('nativeSetPaused(true)', source)
        self.assertNotIn('nativeSetPaused(false)', source)
        self.assertIn('pauseGate.transition(!paused, menuOpen, recoveryDialog?.isShowing == true, leaving)', source)
        self.assertIn('closeMenu() // Resume the same guest', source)
        self.assertIn('if (menuShowing) return@forEach', source)
        self.assertIn('settingsManager = settingsManager', source)

    def test_libretro_result_contracts_and_capability_checks_are_retained(self):
        home = (MENU / 'GameMenuHomeScreen.kt').read_text()
        activity = (MENU / 'GameMenuActivity.kt').read_text()
        for action in ('RESULT_RESET', 'RESULT_ENABLE_AUDIO', 'RESULT_ENABLE_FAST_FORWARD',
                       'RESULT_CHANGE_DISK', 'RESULT_EDIT_TOUCH_CONTROLS', 'RESULT_CHANGE_TILT_CONFIG',
                       'RESULT_CHANGE_WII_CONTROLLER', 'RESULT_QUIT'):
            self.assertIn(action, home)
        for action in ('RESULT_SAVE', 'RESULT_LOAD'):
            self.assertIn(action, activity)
        self.assertIn('states = gameMenuRequest.coreConfig.statesSupported', home)
        self.assertIn('if (gameMenuRequest.fastForwardSupported)', home)
        self.assertIn('if (gameMenuRequest.numDisks > 1)', home)
        self.assertEqual(1, home.count('WiiControllerPicker(gameMenuRequest, onResult)'))
        self.assertIn('GameMenuRoute.GRIPARC', activity)
        self.assertIn('GameMenuRoute.PIVOTARC', activity)

    def test_both_hosts_reuse_the_panel_theme_and_home(self):
        activity = (MENU / 'GameMenuActivity.kt').read_text()
        native = (MENU / 'XboxGameMenu.kt').read_text()
        for text in (activity, native):
            self.assertIn('GameMenuTheme', text)
            self.assertIn('GameMenuPanel', text)
        self.assertIn('SharedGameMenuHome', (MENU / 'GameMenuHomeScreen.kt').read_text())
        self.assertIn('SharedGameMenuHome', native)
        self.assertIn('if (capabilities.showProbe)', native)
        self.assertIn('if (capabilities.rollback)', native)
        self.assertIn('CartridgeLibraryTheme', (MENU / 'SharedGameMenu.kt').read_text())
