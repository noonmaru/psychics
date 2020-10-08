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

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.event.EntityDamageByPsychicEvent
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

object DamageSupport {
    /**
     * * Minecraft의 데미지 계산기
     *
     * [https://www.desmos.com/calculator/tuqrvwfeif]
     *
     * D1 그래프
     *
     * * 방어구 세트 피해량 (20 기준)
     *     * 가죽 = 18.8
     *     * 사슬 = 18.08
     *     * 철 = 16
     *     * 다이아몬드 = 8
     *     * 네더라이트 = 7.2
     * * 인챈트 방어구 세트 피해량 (20 기준, 보호 80%)
     *    * 가죽 = 3.776
     *    * 사슬 = 3.616
     *    * 철 = 3.2
     *    * 다이아몬드 = 1.6
     *    * 네더라이트 = 1.44
     */
    fun calculateMinecraftDamage(damage: Double, armor: Double, armorTough: Double, protection: Double): Double {
        return damage * (1.0 - min(
            20.0,
            max(armor / 5.0, armor - damage / (2.0 + armorTough / 4.0))
        ) / 25.0) * (1.0 - protection * 0.04)
    }

    /**
     * Psychic 데미지 계산기
     *
     * [https://www.desmos.com/calculator/tuqrvwfeif]
     *
     * D2 그래프
     *
     * * 방어구 세트 피해량 (20 기준)
     *    * 가죽 = 17.2
     *    * 사슬 = 15.2
     *    * 철 = 14
     *    * 다이아몬드 = 8.8
     *    * 네더라이트 = 7.2
     * * 인챈트 방어구 세트 피해량 (20 기준, 보호 80%)
     *    * 가죽 = 3.44
     *    * 사슬 = 3.04
     *    * 철 = 2.8
     *    * 다이아몬드 = 1.84
     *    * 네더라이트 = 1.44
     */
    fun calculatePsychicDamage(damage: Double, armor: Double, armorTough: Double, protection: Double): Double {
        return (1.0 - 0.04 * protection) * damage * (1.0 + (-min(armor, armorTough) - armor) / 50.0)
    }

    /**
     * Psychic 데미지 역산기
     */
    fun inversePsychicDamage(damage: Double, armor: Double, armorTough: Double, protection: Double): Double {
        return damage / (1.0 - 0.04 * protection) / (1.0 + (-min(armor, armorTough) - armor) / 50.0)
    }

    // 0레벨일때 최소 공격력 20
    const val minAttackDamage = 1.0
    val maxAttackDamage = inversePsychicDamage(1.0, 20.0, 12.0, 20.0)
    val gapAttackDamage = maxAttackDamage - minAttackDamage

    // 공격력 최대 증가레벨
    private val maxAttackDamageLevel = 40
    private val attackDamagePerExp = gapAttackDamage / calculateTotalExp(maxAttackDamageLevel)

    fun calculateAttackDamage(level: Int): Double {
        return minAttackDamage + attackDamagePerExp * calculateTotalExp(min(maxAttackDamageLevel, level))
    }

    // 마인크래프트 레벨에 의한 토탈 경험치 공식
    private fun calculateTotalExp(level: Int): Double {
        if (level <= 16) {
            return level * level + 6.0 * level
        }
        if (level <= 32) {
            return 2.5 * level * level - 40.5 * level + 360.0
        }
        return 4.5 * level * level - 162.5 * level + 2220.0
    }
}

/**
 * 아이템의 보호 인챈트 수치를 가져옵니다.
 * [Enchantment.PROTECTION_ENVIRONMENTAL]의 경우 두배의 수치를 반환합니다.
 */
fun ItemStack.getProtection(enchantment: Enchantment): Int {
    var protection = getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL)

    if (enchantment != Enchantment.PROTECTION_ENVIRONMENTAL) {
        val enchantmentProtection = getEnchantmentLevel(enchantment) shl 1

        if (protection < enchantmentProtection) {
            protection = enchantmentProtection
        }
    }

    return protection
}

/**
 * 개체 방어구의 보호 인챈트 수치를 가져옵니다.
 *
 * 수치는 [getProtection]을 통해 계산되며 최대 40을 반환합니다.
 */
fun LivingEntity.getProtection(enchantment: Enchantment): Int {
    val armorContents = equipment?.armorContents ?: return 0
    var protection = 0

    armorContents.asSequence().filterNotNull().forEach lit@{ item ->
        protection += item.getProtection(enchantment)
        if (protection >= 40) return@lit
    }

    return min(40, protection)
}

/**
 * 개체에게 능력 피해를 입힙니다.
 *
 * 마인크래프트 피해 계산식이 아닌 [DamageSupport.calculatePsychicDamage]로 계산된 피해를 입힙니다.
 *
 * @return 실제 데미지 값 (이벤트 취소시 -1.0)
 */
fun LivingEntity.psychicDamage(
    ability: Ability<out AbilityConcept>,
    damageType: DamageType,
    damage: Double,
    damager: Player,
    knockbackSource: Location? = damager.location,
    knockbackForce: Double = 0.0
): Double {
    val event = EntityDamageByPsychicEvent(damager, this, damage, ability, damageType, knockbackSource, knockbackForce)

    if (event.callEvent()) {
        return psychicDamageActual(damageType, event.damage, damager, event.knockbackSource, event.knockbackForce)
    }

    return -1.0
}

private fun LivingEntity.psychicDamageActual(
    type: DamageType,
    damage: Double,
    damager: Player,
    knockbackSource: Location? = damager.location,
    knockbackForce: Double = 0.0
): Double {
    val armor = getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0
    val armorTough = getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.value ?: 0.0
    val protection = getProtection(type.protection)
    val actualDamage = DamageSupport.calculatePsychicDamage(damage, armor, armorTough, protection.toDouble())

    killer = damager

    // knockBack
    if (knockbackSource != null && knockbackForce > 0.0) {
        val targetLocation = location
        var force = knockbackForce * 0.5
        val deltaX = knockbackSource.x - targetLocation.x
        val deltaZ = knockbackSource.z - targetLocation.z

        // 대상 넉백 저항으로 인한 넉백 감소
        getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.run { force *= 1.0 - value }

        if (force > 0.0) {
            val velocity = velocity
            val knockBackVelocity = Vector(deltaX, 0.0, deltaZ).normalize().multiply(force)
            val newVelocity = Vector().apply {
                // 수평 속도를 절반 줄이고 넉백 속도 적용
                x = velocity.x / 2.0 - knockBackVelocity.x
                z = velocity.z / 2.0 - knockBackVelocity.z
                // 대상이 공중에 있을경우 수직 속도를 절반 줄이고 넉백 힘 만큼 적용
                y = if (isOnGround) min(0.4, velocity.y / 2.0 + force) else velocity.y
            }

            this.velocity = newVelocity
        }
    }

    if (this is Mob && target == null)
        target = damager

    noDamageTicks = 0
    damage(actualDamage)

    return actualDamage
}