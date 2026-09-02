package com.example.runtracker.domain.beacon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BeaconCodeTest {

    @Test
    fun `random code is valid and avoids ambiguous chars`() {
        repeat(200) {
            val code = BeaconCode.random(Random(it.toLong()))
            assertEquals(BeaconCode.LENGTH, code.length)
            assertTrue(BeaconCode.isValid(code))
            assertTrue(code.none { c -> c in "IO01" })
        }
    }

    @Test
    fun `normalize uppercases and strips noise`() {
        assertEquals("ABC234", BeaconCode.normalize("  abc-234 "))
        assertEquals("ABDEFG", BeaconCode.normalize("ab*de.fg"))
    }

    @Test
    fun `isValid rejects wrong length or bad chars`() {
        assertFalse(BeaconCode.isValid("ABC23"))
        assertFalse(BeaconCode.isValid("ABC230")) // 0 not in alphabet
        assertFalse(BeaconCode.isValid("abc234")) // lowercase
        assertTrue(BeaconCode.isValid("ABC234"))
    }
}
