package dev.nexaddons.skyblock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HypixelServerDetectorTest {
    @Test
    fun `detects hypixel hosts`() {
        assertTrue(HypixelServerDetector.matches("hypixel.net"))
        assertTrue(HypixelServerDetector.matches("mc.hypixel.net"))
        assertTrue(HypixelServerDetector.matches("play.hypixel.net:25565"))
        assertTrue(HypixelServerDetector.matches("https://mc.hypixel.net/lobby"))
    }

    @Test
    fun `rejects lookalike hosts`() {
        assertFalse(HypixelServerDetector.matches(null))
        assertFalse(HypixelServerDetector.matches(""))
        assertFalse(HypixelServerDetector.matches("hypixel.net.evil.example"))
        assertFalse(HypixelServerDetector.matches("not-hypixel.net"))
    }

    @Test
    fun `normalizes addresses`() {
        assertEquals("mc.hypixel.net", HypixelServerDetector.normalizedHost("  MC.HYPIXEL.NET:25565.  "))
        assertEquals("localhost", HypixelServerDetector.normalizedHost("localhost:25565"))
    }
}
