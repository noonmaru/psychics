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

package com.github.noonmaru.psychic

import com.github.noonmaru.psychic.task.PsychicScheduler
import com.github.noonmaru.psychic.utils.currentTicks
import com.github.noonmaru.psychic.utils.findString
import com.github.noonmaru.tap.config.applyConfig
import com.github.noonmaru.tap.event.RegisteredEntityListener
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

class Psychic internal constructor(val spec: PsychicSpec) {

    var prevMana: Double = 0.0

    var mana: Double = 0.0

    var manaRegenPerTick: Double = spec.manaRegenPerSec / 50.0

    var prevRegenManaTick: Int = 0

    private val manaBar: BossBar?

    val abilities: List<Ability>

    private val scheduler = PsychicScheduler()

    private val projectiles = ProjectileManager()

    lateinit var esper: Esper
        internal set

    private val playerListeners = ArrayList<RegisteredEntityListener>()

    var enabled: Boolean = false
        set(value) {
            checkState()

            if (field != value) {
                field = value

                abilities.forEach { it.runCatching { if (value) onEnable() else onDisable() } }
            }
        }

    var valid: Boolean = false
        private set

    init {
        abilities = spec.abilities.let {
            val list = ArrayList<Ability>()
            for (abilitySpec in it) {
                list += abilitySpec.abilityClass.newInstance().apply {
                    spec = spec
                    psychic = psychic
                    onInitialize()
                }
            }

            ImmutableList.copyOf(list)
        }

        manaBar = if (spec.mana > 0) Bukkit.createBossBar("MANA", BarColor.BLUE, BarStyle.SOLID) else null

    }

    internal fun register(esper: Esper) {
        checkState()
        Preconditions.checkState(!this::esper.isInitialized, "Already registered $this")

        this.esper = esper
        this.valid = true
        this.prevRegenManaTick = currentTicks

        registerPlayerEvents(WandListener())

        for (ability in abilities) {
            try {
                ability.onRegister()
            } catch (t: Throwable) {
                Psychics.logger.info("Failed to register $ability")
                t.printStackTrace()
            }
        }

        playerListeners.trimToSize()
    }

    inner class WandListener : Listener {
        @EventHandler
        fun onInteract(event: PlayerInteractEvent) {
            if (event.action != Action.PHYSICAL && event.hand == EquipmentSlot.HAND) {
                event.item?.let { castByWand(it) }
            }
        }

        @EventHandler
        fun onInteractEntity(event: PlayerInteractEntityEvent) {
            castByWand(event.player.inventory.itemInMainHand)
        }
    }

    private fun castByWand(item: ItemStack) {
        getAbilityByWand(item)?.let { ability ->
            if (ability is CastableAbility) {
                ability.tryCast()
            }
        }
    }

    fun getAbilityByWand(item: ItemStack): Ability? {
        return abilities.find { ability -> ability.spec.wand?.isSimilar(item) ?: false }
    }

    internal fun unregister() {
        checkState()

        this.valid = false

        for (playerListener in playerListeners) {
            playerListener.unregister()
        }

        playerListeners.clear()

        scheduler.cancelAll()
        projectiles.removeAll()
    }

    fun registerPlayerEvents(listener: Listener) {
        checkState()

        playerListeners += Psychics.entityEventBus.registerEvents(esper.player, listener)
    }

    fun runTask(runnable: Runnable, delay: Long) {
        checkState()

        scheduler.runTask(runnable, delay)
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long) {
        checkState()

        scheduler.runTaskTimer(runnable, delay, period)
    }

    fun launch(projectile: Projectile) {
        checkState()

        projectiles.add(projectile)
    }

    fun checkState() {
        Preconditions.checkState(valid, "Invalid $this")
    }

    internal fun update() {
        regenMana()

        scheduler.run()
        projectiles.updateAll()

        val current = currentTicks
        val queue = channelQueue

        while (queue.isNotEmpty()) {
            val channel = queue.peek()

            if (current < channel.castTick)
                break

            queue.remove()
            channel.cast()
        }

        if (this.prevMana != this.mana) {
            this.prevMana = this.mana
            TODO("Mana update")
        }
    }

    private fun regenMana() {
        if (manaRegenPerTick > 0) {

            val current = currentTicks
            val elapsed = current - prevRegenManaTick

            val max = spec.mana
            if (mana < max) {
                val newMana = min(mana + manaRegenPerTick * elapsed, max)

                if (mana != newMana) {
                    mana = newMana
                    manaBar?.progress = mana / spec.mana
                }
            }
        }
    }

    private val channelQueue = PriorityQueue<CastableAbility.Channel>()

    internal fun startChannel(channel: CastableAbility.Channel) {
        channelQueue += channel
    }
}


class PsychicSpec(specFile: File) {

    val name: String

    val displayName: String

    val mana: Double

    val manaRegenPerSec: Double

    val abilities: List<AbilitySpec>

    init {
        val config = YamlConfiguration.loadConfiguration(specFile)

        name = config.findString("name")
        displayName = config.getString("display-name") ?: name
        mana = max(config.getDouble("mana"), 0.0)
        manaRegenPerSec = config.getDouble("mana-regen-per-sec")

        abilities = config.getConfigurationSection("abilities")?.run {
            val list = ArrayList<AbilitySpec>()

            var absent = false

            for ((abilityName, value) in config.getValues(false)) {
                if (value is ConfigurationSection) {
                    Psychics.storage.abilityModels[abilityName]?.let { abilityModel ->
                        list += abilityModel.specClass.newInstance().apply {
                            model = abilityModel
                            psychicSpec = this@PsychicSpec
                            if (applyConfig(value))
                                absent = true
                            onInitialize()
                        }

                    } ?: throw NullPointerException("Not found AbilityModel for '$abilityName'")
                }
            }

            if (absent)
                config.save(specFile)

            ImmutableList.copyOf(list)

        } ?: ImmutableList.of()
    }
}