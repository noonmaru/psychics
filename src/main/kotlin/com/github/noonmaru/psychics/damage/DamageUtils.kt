/*
 * Copyright (c) 2020 Noonmaru
 *
 *  Licensed under the General Public License, Version 3.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.noonmaru.psychics.damage

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import kotlin.math.min

fun ItemStack.getProtection(enchantment: Enchantment?): Int {
    var protection = getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL)

    if (enchantment != null && enchantment != Enchantment.PROTECTION_ENVIRONMENTAL) {
        val enchantmentProtection = getEnchantmentLevel(enchantment) shl 1

        if (protection < enchantmentProtection) {
            protection = enchantmentProtection
        }
    }

    return protection
}

fun LivingEntity.getProtection(enchantment: Enchantment?): Int {
    val armorContents = equipment?.armorContents
    var protection = 0

    if (armorContents != null) {
        for (item in armorContents) {
            if (item != null) {
                val itemProtection = item.getProtection(enchantment)

                protection += itemProtection

                if (protection >= 40) {
                    break
                }
            }
        }
    }

    return min(40, protection)
}