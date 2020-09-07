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

import com.github.noonmaru.psychics.task.TickScheduler
import com.github.noonmaru.psychics.task.TickTask
import com.github.noonmaru.psychics.util.Tick
import com.github.noonmaru.tap.event.RegisteredEntityListener
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.FakeProjectileManager
import com.github.noonmaru.tap.ref.UpstreamReference
import com.google.common.collect.ImmutableList
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.BlockData
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.max

class Psychic(
    val concept: PsychicConcept
) {
    var mana = 0.0
        set(value) {
            checkState()

            if (value != field) {
                val max = concept.mana
                field = value.coerceIn(0.0, max)
                updateManaBar()
            }
        }

    var ticks = 0L
        private set

    val abilities: List<Ability<*>>

    var channeling: Channel? = null
        private set

    var enabled = false
        set(value) {
            checkState()

            if (field != value) {
                field = value

                if (value) {
                    onEnable()
                } else {
                    onDisable()
                }
            }
        }

    var valid = true
        private set

    private lateinit var esperRef: UpstreamReference<Esper>

    val esper: Esper
        get() = esperRef.get()

    private var manaBar: BossBar? = null

    private lateinit var castingBar: BossBar

    private lateinit var scheduler: TickScheduler

    private lateinit var projectileManager: FakeProjectileManager

    private lateinit var listeners: ArrayList<RegisteredEntityListener>

    private lateinit var fakeEntities: MutableSet<FakeEntity>

    private var prevUpdateTicks = 0L

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

        val player = esper.player

        esperRef = UpstreamReference(esper)
        castingBar = Bukkit.createBossBar(null, BarColor.WHITE, BarStyle.SOLID).apply {
            addPlayer(player)
            isVisible = false
        }
        scheduler = TickScheduler()
        projectileManager = FakeProjectileManager()
        listeners = arrayListOf()
        fakeEntities = Collections.newSetFromMap(WeakHashMap<FakeEntity, Boolean>())

        if (concept.mana > 0.0) {
            manaBar = Bukkit.createBossBar(null, concept.manaColor, BarStyle.SEGMENTED_10).apply {
                addPlayer(player)
                isVisible = true
            }
        }

        updateManaBar()

        for (ability in abilities) {
            ability.runCatching { onAttach() }
        }
    }

    private fun detach() {
        manaBar?.removeAll()
        castingBar.removeAll()

        for (ability in abilities) {
            ability.runCatching {
                cooldownTicks = 0
                onDetach()
            }
        }
        esperRef.clear()
    }

    private fun onEnable() {
        enabled = true
        prevUpdateTicks = Tick.currentTicks

        for (ability in abilities) {
            ability.runCatching { onEnable() }
        }
    }

    private fun onDisable() {
        castingBar.isVisible = false
        scheduler.cancelAll()
        projectileManager.clear()
        listeners.run {
            for (registeredEntityListener in this) {
                registeredEntityListener.unregister()
            }
            clear()
        }
        fakeEntities.run {
            for (fakeEntity in this) {
                fakeEntity.remove()
            }
            clear()
        }

        for (ability in abilities) {
            ability.runCatching { onDisable() }
        }
    }

    private fun updateManaBar() {
        manaBar?.let { bar ->
            val current = mana
            val max = concept.mana

            bar.progress = current / max
            bar.setTitle("${current.toInt()} / ${max.toInt()}")
        }
    }

    companion object {
        internal const val NAME = "name"
        private const val MANA = "mana"
        private const val TICKS = "ticks"
        private const val ENABLED = "enabled"
        private const val ABILITIES = "abilities"
    }

    internal fun save(config: ConfigurationSection) {
        config[NAME] = concept.name
        config[MANA] = mana
        config[TICKS] = ticks
        config[ENABLED] = enabled

        val abilitiesSection = config.createSection(ABILITIES)

        for (ability in abilities) {
            val abilityName = ability.concept.name
            val abilitySection = abilitiesSection.createSection(abilityName)

            ability.save(abilitySection)
        }
    }

    internal fun load(config: ConfigurationSection) {
        mana = config.getDouble(MANA).coerceIn(0.0, concept.mana)
        ticks = max(0, config.getLong(TICKS))

        config.getConfigurationSection(ABILITIES)?.let { abilitiesSection ->
            for (ability in abilities) {
                val abilityName = ability.concept.name
                val abilitySection = abilitiesSection.getConfigurationSection(abilityName)

                if (abilitySection != null)
                    ability.load(abilitySection)
            }
        }

        enabled = config.getBoolean(ENABLED)
    }

    fun getAbilityByWand(item: ItemStack): Ability<*>? {
        for (ability in abilities) {
            val wand = ability.concept._wand

            if (wand != null && wand.isSimilar(item))
                return ability
        }

        return null
    }

    fun runTask(runnable: Runnable, delay: Long): TickTask {
        checkState()
        checkEnabled()

        return scheduler.runTask(runnable, delay)
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): TickTask {
        checkState()
        checkEnabled()

        return scheduler.runTaskTimer(runnable, delay, period)
    }

    fun launchProjectile(location: Location, projectile: PsychicProjectile) {
        checkState()
        checkEnabled()

        projectile.psychic = this
        projectileManager.launch(location, projectile)
    }

    fun spawnFakeEntity(location: Location, entityClass: Class<out Entity>): FakeEntity {
        checkState()
        checkEnabled()

        val fakeEntity = Psychics.fakeEntityServer.spawnEntity(location, entityClass)
        fakeEntities.add(fakeEntity)

        return fakeEntity
    }

    fun spawnFallingBlock(location: Location, blockData: BlockData): FakeEntity {
        checkState()
        checkEnabled()

        val fakeEntity = Psychics.fakeEntityServer.spawnFallingBlock(location, blockData)
        fakeEntities.add(fakeEntity)

        return fakeEntity
    }

    fun consumeMana(amount: Double): Boolean {
        if (mana > amount) {
            mana -= amount

            return true
        }

        return false
    }

    internal fun startChannel(ability: ActiveAbility<*>, castingTicks: Long, target: Any?) {
        interruptChannel()

        channeling = Channel(ability, castingTicks, target)
        castingBar.apply {
            val abilityConcept = ability.concept
            setTitle("${ChatColor.BOLD}${concept.displayName}")
            color = abilityConcept.castingBarColor ?: BarColor.YELLOW
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
        enabled = false
        detach()
        valid = false
    }

    internal fun update() {
        val currentTicks = Tick.currentTicks
        val elapsedTicks = currentTicks - prevUpdateTicks
        prevUpdateTicks = currentTicks
        ticks += elapsedTicks

        regenMana(elapsedTicks)
        regenHealth(elapsedTicks)

        scheduler.run()
        projectileManager.update()

        channeling?.let { channel ->
            val remainTicks = channel.remainTicks

            castingBar.progress = 1.0 - remainTicks.toDouble() / channel.ability.concept.castingTicks.toDouble()

            if (remainTicks > 0) {
                channel.channel()
            } else {
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
                    maxHealth
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

class Channel internal constructor(val ability: ActiveAbility<*>, castingTicks: Long, val target: Any? = null) {
    private val channelTick = Tick.currentTicks + castingTicks

    val remainTicks
        get() = max(0, channelTick - Tick.currentTicks)

    internal fun channel() {
        ability.runCatching { onChannel(target) }
    }

    internal fun cast() {
        ability.runCatching { onCast(target) }
    }

    internal fun interrupt() {
        ability.runCatching { onInterrupt(target) }
    }
}