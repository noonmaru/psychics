package com.github.noonmaru.psychics.item

import com.github.noonmaru.psychics.BukkitInitialization
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftInventoryCustom
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PsychicItemTest {
    @Before
    fun setup() {
        BukkitInitialization.initialize()
    }

    @Test
    fun test() {
        val item = ItemStack(Material.STICK)
        item.isPsychicbound = true

        assertTrue(item.isPsychicbound)
        assertEquals(listOf(PsychicItem.boundTag), item.itemMeta?.lore)

        val inv = CraftInventoryCustom(Mockito.mock(InventoryHolder::class.java), 9).apply {
            setItem(0, ItemStack(Material.STICK))
            setItem(1, item.clone())
        }

        assertTrue(inv.getItem(1)?.isPsychicbound ?: false)
        inv.removeAllPsychicbounds()
        assertNull(inv.getItem(1))
    }
}