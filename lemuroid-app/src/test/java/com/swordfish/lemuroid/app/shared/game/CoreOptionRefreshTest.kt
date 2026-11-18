package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreVariable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreOptionRefreshTest {
    @Test
    fun unchangedValuesDoNotDispatch() {
        val desired = listOf(CoreVariable("video", "enabled"))
        assertTrue(CoreOptionRefresh.changed(desired, mapOf("video" to "enabled")).isEmpty())
    }

    @Test
    fun changedAndUnreportedValuesStillDispatch() {
        val desired = listOf(CoreVariable("video", "enabled"), CoreVariable("audio", "disabled"))
        assertEquals(desired, CoreOptionRefresh.changed(desired, mapOf("video" to "disabled")))
        assertEquals(listOf(desired.last()), CoreOptionRefresh.changed(desired, mapOf("video" to "enabled")))
    }
}
