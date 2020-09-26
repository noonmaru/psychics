package com.github.noonmaru.psychics.item

import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object PsychicItem {
    val boundTag = "${ChatColor.RED}${ChatColor.BOLD}Psychicbound"
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

        if (item != null && item.isPsychicbound) {
            setItem(i, null)
        }
    }
}

fun Inventory.addItemNonDuplicate(items: Collection<ItemStack>) {
    out@ for (item in items) {
        for (invItem in this) {
            if (invItem == null)
                continue
            if (invItem.isSimilarLore(item))
                continue@out
        }

        addItem(item)
    }
}

private fun ItemStack.isSimilarLore(other: ItemStack): Boolean {
    val meta = itemMeta
    val otherMeta = other.itemMeta

    return type == other.type && data == other.data && meta.displayName == itemMeta.displayName && meta.lore == otherMeta.lore
}