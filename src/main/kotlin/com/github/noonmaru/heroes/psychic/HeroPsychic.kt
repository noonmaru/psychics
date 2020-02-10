/*
 * Copyright (c) 2020 Noonmaru
 *
 * Licensed under the General Public License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.noonmaru.heroes.psychic

import com.github.noonmaru.heroes.Heroes
import com.github.noonmaru.tap.event.RegisteredEntityListener
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.math.min

class HeroPsychic(val psychic: Psychic) {

    var mana: Int = 0

    var maxMana: Int = psychic.mana

    var manaRegen: Int = psychic.manaRegen

    var manaRegenTick: Int = 0

    val abilities: List<HeroAbility> = psychic.abilities.map {
        it.createHeroAbility().apply {
            ability = it
            heroPsychic = this@HeroPsychic
        }
    }

    val castables: List<Pair<ItemStack, Castable>> = abilities.run {
        val list = ArrayList<Pair<ItemStack, Castable>>(this.count())
        for (ability in filter { it.ability is Castable }) {
            ability.ability.wand?.let { wand ->
                list += Pair(wand, ability as Castable)
            }
        }

        ImmutableList.copyOf(list)
    }

    private val listeners = ArrayList<RegisteredEntityListener>()

    var enabled: Boolean = false
        set(value) {
            checkState()

            if (field != value) {
                field = value

                for (heroAbility in abilities) { // Call onEnable
                    heroAbility.ability.run {
                        try {
                            if (value && !heroAbility.isLockdown) {
                                onEnable(hero, this@HeroPsychic, heroAbility)
                            } else onDisable(hero, this@HeroPsychic, heroAbility)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

    /**
     * 유효 여부
     *
     * **true** = 유효 ([Hero]에게 등록되어있음)
     *
     * **false** = 무효 ([Hero]부터 해제되었거나 등록되지 않음)
     */
    var valid = false
        private set

    lateinit var hero: Hero

    internal fun register(hero: Hero) {
        Preconditions.checkState(
            this::hero.isInitialized,
            "Already used HeroPsychic @${System.identityHashCode(this).toString(16)} hero=${this.hero.player.name} _hero=${hero.player.name}"
        )

        this.hero = hero
        this.valid = true

        for (heroAbility in abilities) {
            try {
                heroAbility.ability.onRegister(hero, this, heroAbility)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class WandListener : Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        fun onInteract(event: PlayerInteractEvent) {
            if (event.action != Action.PHYSICAL && event.hand == EquipmentSlot.HAND) {

            }
        }
    }

    internal fun unregister() {
        checkState()

        valid = false

        listeners.let {
            for (registeredEntityListener in it) {
                registeredEntityListener.unregister()
            }
            it.clear()
        }

        for (heroAbility in abilities) {
            try {
                heroAbility.ability.onUnregister(hero, this, heroAbility)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * [HeroPsychic]의 상태를 확인합니다.
     *
     * [HeroPsychic.valid]가 **false**인 경우 [IllegalStateException]을 발생시킵니다.
     *
     * @exception IllegalStateException
     */
    fun checkState() {
        Preconditions.checkState(valid, "Invalid $javaClass @${System.identityHashCode(this).toString(0x10)}")
    }

    fun registerEvents(listener: Listener) {
        checkState()

        listeners += Heroes.entityEventManager.registerEvents(hero.player, listener)
    }

    internal fun onUpdate() {
        if (maxMana > 0 && manaRegen > 0)
            regenMana()
    }

    private fun regenMana() {
        if (mana < maxMana) {
            if (++manaRegenTick >= psychic.manaRegenTick) {
                mana = min(psychic.mana, mana + manaRegen)
            }
        } else {
            manaRegenTick = 0
        }
    }
}