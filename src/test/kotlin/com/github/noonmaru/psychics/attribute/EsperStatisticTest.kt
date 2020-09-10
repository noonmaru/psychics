package com.github.noonmaru.psychics.attribute


import org.junit.Assert.assertEquals
import org.junit.Test

class EsperStatisticTest {
    @Test
    fun test() {
        assertEquals(1.0, EsperStatistic.calculateAttachDamageByLevel(0), 0.01)
        assertEquals(26.8, EsperStatistic.calculateAttachDamageByLevel(Int.MAX_VALUE), 0.01)
    }

}