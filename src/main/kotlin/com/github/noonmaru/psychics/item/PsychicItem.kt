package com.github.noonmaru.psychics.item

import com.github.noonmaru.psychics.isPsychicWrittenBook
import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object PsychicItem {
    val boundTag = "${ChatColor.RED}${ChatColor.BOLD}능력 귀속"
}

var ItemStack.isPsychicbound
    get() = itemMeta?.lore?.let { PsychicItem.boundTag in it } ?: false
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

var ItemMeta.isPsychicbound
    get() = lore?.let { PsychicItem.boundTag in it } ?: false
    set(value) {
        val lore = lore ?: ArrayList<String>(1)

        if (value) {
            if (PsychicItem.boundTag !in lore) {
                lore.add(PsychicItem.boundTag)
                this.lore = lore
            }
        } else {
            if (lore.remove(PsychicItem.boundTag)) {
                this.lore = lore
            }
        }
    }

fun Inventory.removeAllPsychicbounds() {
    for (i in 0 until count()) {
        val item = getItem(i)

        if (item != null && item.isPsychicbound && !item.isPsychicWrittenBook) {
            setItem(i, null)
        }
    }
}