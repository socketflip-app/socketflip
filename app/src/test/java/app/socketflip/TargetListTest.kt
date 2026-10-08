package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetListTest {
    @Test fun roundTrip() {
        val list = listOf("com.example.one", "org.example.two")
        assertEquals(list, Prefs.parseTargets(Prefs.joinList(list), null))
    }

    @Test fun emptyListStaysEmptyAndIgnoresTheOldTarget() {
        // Once the list has been written, an old single target must not come back.
        assertEquals(emptyList<String>(), Prefs.parseTargets("", "com.example.old"))
    }

    @Test fun blankLinesAreDropped() {
        assertEquals(listOf("a.b", "c.d"), Prefs.parseTargets("a.b\n\nc.d\n", null))
    }

    @Test fun migratesTheSingleTargetFromBefore110() {
        assertEquals(listOf("com.example.old"), Prefs.parseTargets(null, "com.example.old"))
        assertEquals(emptyList<String>(), Prefs.parseTargets(null, null))
        assertEquals(emptyList<String>(), Prefs.parseTargets(null, ""))
    }

    @Test fun everythingTickedWhenNothingWasEverTicked() {
        val all = listOf("a.b", "c.d")
        assertEquals(all, Prefs.parseChecked(null, all))
    }

    @Test fun tickedKeepsOnlyAppsStillInTheList() {
        assertEquals(listOf("c.d"), Prefs.parseChecked("gone.app\nc.d", listOf("a.b", "c.d")))
        assertEquals(emptyList<String>(), Prefs.parseChecked("", listOf("a.b")))
    }
}
