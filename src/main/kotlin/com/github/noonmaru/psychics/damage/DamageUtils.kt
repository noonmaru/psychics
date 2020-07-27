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

import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.max
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

fun LivingEntity.damage(
    type: DamageType,
    damage: Double,
    source: Player? = null,
    knockBackLocation: Location? = source?.location,
    knockBack: Double = 0.0
) {
    val armor = getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0
    val armorTough = getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.value ?: 0.0
    val protection = getProtection(type.protection)

    val damageByArmor = 1.0 - min(20.0, max(armor / 5.0, armor - damage / (2.0 + armorTough / 4.0))) / 25.0
    val damageByProtection = 1.0 - protection / 50.0

    val actualDamage = damage * damageByArmor * damageByProtection

    if (source != null)
        killer = source

    // knockBack
    if (knockBackLocation != null && knockBack > 0.0) {
        val targetLocation = location

        var force = knockBack * 0.5
        val deltaX = knockBackLocation.x - targetLocation.x
        val deltaZ = knockBackLocation.z - targetLocation.z

        getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.let { force *= 1.0 - it.value }

        if (force > 0.0) {
            val velocity = velocity
            val knockBackVelocity = Vector(deltaX, 0.0, deltaZ).normalize().multiply(force)
            val newVelocity = Vector(
                velocity.x / 2.0 - knockBackVelocity.x,
                if (isOnGround) min(0.4, velocity.y / 2.0 + force) else velocity.y,
                velocity.z / 2.0 - knockBackVelocity.z
            )

            this.velocity = newVelocity
        }
    }

    damage(actualDamage)
}