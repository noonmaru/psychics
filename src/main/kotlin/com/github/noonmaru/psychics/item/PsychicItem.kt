package com.github.noonmaru.psychics.item

import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object PsychicItem {
    val boundTag = "${ChatColor.RED}${ChatColor.BOLD} 능력 귀속"
}

var ItemStack.isPsychicbound
    get() = itemMeta.lore?.let { PsychicItem.boundTag in it } ?: false
    set(value) {
        val meta = itemMeta
        val lore = meta.lore ?: ArrayList<String>(1)

        if (value) {
            if (PsychicItem.boundTag !in lore) {
                lore.add(PsychicItem.boundTag)
                meta.lore = lore
                itemMeta = meta
            }
        } else {
            if (lore.remove(PsychicItem.boundTag)) {
                meta.lore = lore
                itemMeta = meta
            }
        }
    }

fun Inventory.removeAllPsychicbounds() {
    for (i in 0 until count()) {
        val item = getItem(i)

        if (item != null && item.isPsychicbound) {
            setItem(i, null)
        }
    }
}