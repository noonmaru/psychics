package com.github.noonmaru.psychics.damage

import org.junit.Assert.assertEquals
import org.junit.Test

class DamageSupportTest {
    @Test
    fun test() {
        val minecraftDamage = DamageSupport.calculateMinecraftDamage(100.0, 20.0, 12.0, 20.0)
        val psychicDamage = DamageSupport.calculatePsychicDamage(100.0, 20.0, 12.0, 20.0)

        assertEquals(16.8, minecraftDamage, 0.1)
        assertEquals(7.2, psychicDamage, 0.1)
    }

}