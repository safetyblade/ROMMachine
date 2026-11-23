package com.swordfish.lemuroid.app.mobile.feature.product

import org.junit.Assert.assertTrue
import org.junit.Test

class SafeSaveCoverageTextTest {
    @Test fun boundedBackupExplainsRecognizedPs2CardsAndNativeExclusions() {
        assertTrue(SAFESAVE_BACKUP_COVERAGE.contains("PS1"))
        assertTrue(SAFESAVE_BACKUP_COVERAGE.contains("ARMSX2 PS2"))
        assertTrue(SAFESAVE_BACKUP_COVERAGE.contains("Other native storage"))
        assertTrue(SAFESAVE_BACKUP_COVERAGE.contains("Unassigned Backups"))
    }
}
