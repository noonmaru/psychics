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

package com.github.noonmaru.psychics

import com.github.noonmaru.psychics.task.PsychicScheduler
import com.github.noonmaru.psychics.task.PsychicTask
import com.github.noonmaru.psychics.util.Tick
import com.github.noonmaru.tap.event.RegisteredEntityListener
import com.github.noonmaru.tap.fake.FakeProjectileManager
import com.github.noonmaru.tap.ref.UpstreamReference
import com.google.common.collect.ImmutableList
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import kotlin.math.max

class Psychic(
    val concept: PsychicConcept
) {
    var mana = 0.0

    private var ticks = 0L

    val abilities: List<Ability<*>>

    var channeling: Channel? = null
        private set

    var enabled = false
        private set

    var valid = true
        private set

    private lateinit var esperRef: UpstreamReference<Esper>

    val esper: Esper
        get() = esperRef.get()

    private var manaBar: BossBar? = null

    private lateinit var castingBar: BossBar

    private lateinit var scheduler: PsychicScheduler

    private lateinit var projectileManager: FakeProjectileManager

    private lateinit var listeners: ArrayList<RegisteredEntityListener>

    init {
        abilities = ImmutableList.copyOf(concept.abilityConcepts.map { concept ->
            concept.createAbilityInstance().apply {
                initPsychic(this@Psychic)
                runCatching { onInitialize() }
            }
        })
    }

    internal fun attach(esper: Esper) {
        require(!this::esperRef.isInitialized) { "Cannot redefine epser" }

        esperRef = UpstreamReference(esper)
        castingBar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID)
        scheduler = PsychicScheduler()
        projectileManager = FakeProjectileManager()
        listeners = arrayListOf()

        if (concept.mana > 0.0) {
            manaBar = Bukkit.createBossBar(null, concept.manaColor, BarStyle.SEGMENTED_10)
        }

        for (ability in abilities) {
            ability.runCatching { onAttach() }
        }
    }

    internal fun detach() {
        for (ability in abilities) {
            ability.runCatching { onDetach() }
        }
        esperRef.clear()
    }

    fun enable() {
        checkState()

        if (enabled) return

        enabled = true
        ticks = Tick.currentTicks

        for (ability in abilities) {
            ability.runCatching { onEnable() }
        }
    }

    fun disable() {
        checkState()

        if (!enabled) return

        enabled = false

        castingBar.isVisible = false
        scheduler.cancelAll()
        projectileManager.clear()
        listeners.run {
            for (registeredEntityListener in this) {
                registeredEntityListener.unregister()
            }
            clear()
        }

        for (ability in abilities) {
            ability.runCatching { onDisable() }
        }
    }

    fun runTask(runnable: Runnable, delay: Long): PsychicTask {
        checkState()
        checkEnabled()

        return scheduler.runTask(runnable, delay)
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): PsychicTask {
        checkState()
        checkEnabled()

        return scheduler.runTaskTimer(runnable, delay, period)
    }

    fun launchProjectile(location: Location, projectile: PsychicProjectile) {
        checkState()
        checkEnabled()

        projectileManager.launch(location, projectile)
    }

    internal fun startChannel(ability: ActiveAbility<*>, castingTicks: Int, target: Any?) {
        interruptChannel()

        channeling = Channel(ability, castingTicks, target)
        castingBar.apply {
            setTitle(ability.concept.displayName)
            progress = 0.0
            isVisible = true
        }
    }

    fun interruptChannel() {
        channeling?.let { channel ->
            channel.interrupt()
            channeling = null
        }
    }

    internal fun destroy() {
        disable()
        detach()
        valid = false
    }

    internal fun update() {
        val elapsedTicks = Tick.currentTicks - ticks

        regenMana(elapsedTicks)
        regenHealth(elapsedTicks)

        scheduler.run()
        projectileManager.update()

        channeling?.let { channel ->
            val remainTicks = channel.remainTicks

            castingBar.progress = 1.0 - remainTicks.toDouble() / channel.ability.concept.castingTicks.toDouble()

            if (remainTicks <= 0) {
                castingBar.setTitle(null)
                castingBar.isVisible = false
                channeling = null
                channel.cast()
            }
        }
    }

    private fun regenMana(elapsedTicks: Long) {
        mana = (mana + concept.manaRegenPerTick * elapsedTicks).coerceIn(0.0, concept.mana)
    }

    private fun regenHealth(elapsedTicks: Long) {
        val player = esper.player
        val health = player.health

        if (health > 0.0) {
            val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            player.health =
                (player.health + concept.healthRegenPerTick * elapsedTicks).coerceIn(
                    0.0,
                    maxHealth + concept.healthBonus
                )
        }
    }

    fun checkState() {
        require(valid) { "Invalid Psychic@${System.identityHashCode(this).toString(16)}" }
    }

    fun checkEnabled() {
        require(enabled) { "Disabled Psychic@${System.identityHashCode(this).toString(16)}" }
    }
}

class Channel internal constructor(val ability: ActiveAbility<*>, castingTicks: Int, val target: Any? = null) {
    internal val channelTick = Tick.currentTicks + castingTicks

    val remainTicks
        get() = max(0, channelTick - Tick.currentTicks)

    internal fun cast() {
        ability.runCatching { onCast(target) }
    }

    internal fun interrupt() {
        ability.runCatching { onInterrupt(target) }
    }
}