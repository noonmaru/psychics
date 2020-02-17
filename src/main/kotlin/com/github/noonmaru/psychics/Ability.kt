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

package com.github.noonmaru.psychics

import com.github.noonmaru.psychics.utils.currentTicks
import com.github.noonmaru.psychics.utils.findString
import com.github.noonmaru.psychics.utils.processTemplatesAll
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.config.RangeDouble
import com.github.noonmaru.tap.config.RangeInt
import com.google.common.collect.ImmutableList
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import java.io.File

/**
 * Ability.jar의 정보를 기술하는 클래스입니다.
 */
class AbilityDescription internal constructor(config: ConfigurationSection) {

    val name = config.findString("name")

    val main = config.findString("main")

    val version = config.findString("version")

    val authors: List<String> = ImmutableList.copyOf(config.getStringList("authors"))

}

/**
 * Ability.jar의 데이터 컨테이너입니다.
 */
class AbilityModel internal constructor(
    val file: File,
    val description: AbilityDescription,
    val classLoader: ClassLoader,
    val specClass: Class<out AbilitySpec>
)

/**
 * 능력의 기본적인 정보를 기술하는 클래스입니다.
 *
 * 스탯이나 설명등 정보를 입력하세요
 */
@Name("ability")
abstract class AbilitySpec {

    lateinit var model: AbilityModel
        private set

    lateinit var psychicSpec: PsychicSpec

    @Config
    lateinit var displayName: String

    @Config
    var type: AbilityType = AbilityType.PASSIVE
        protected set

    @Config
    @RangeInt(min = 0)
    var cooldownTick: Int = 0
        protected set

    @Config
    @RangeInt(min = 0)
    var cost: Double = 0.0
        protected set

    @Config
    @RangeInt(min = 0)
    var channelDuration: Int = 0
        protected set

    @Config
    var channelInterruptible: Boolean = false
        protected set

    @Config
    @RangeDouble(min = 0.0)
    var range: Double = 0.0
        protected set

    @Config("wand", required = false)
    internal var _wand: ItemStack? = null

    var wand
        get() = _wand?.clone()
        protected set(value) {
            _wand = value?.clone()
        }

    @Config
    var description: List<String> = ImmutableList.of()
        private set

    abstract val abilityClass: Class<out Ability>

    internal fun initialize(
        model: AbilityModel,
        psychicSpec: PsychicSpec,
        config: ConfigurationSection
    ) {
        this.model = model
        this.psychicSpec = psychicSpec

        if (CastableAbility::class.java.isAssignableFrom(abilityClass))
            type = AbilityType.CASTING

        description = ImmutableList.copyOf(description.processTemplatesAll(config))
    }

    fun onInitialize() {}
}

enum class AbilityType {
    MOVEMENT,
    CASTING,
    SPELL,
    PASSIVE
}

/**
 *
 */
abstract class Ability {

    lateinit var spec: AbilitySpec
        internal set

    lateinit var psychic: Psychic
        internal set

    var cooldownTick: Int = 0
        get() {
            return (field - currentTicks).coerceIn(0, Int.MAX_VALUE)
        }
        set(value) {
            checkState()

            field = currentTicks + value.coerceIn(0, Int.MAX_VALUE)
            spec._wand?.let { psychic.esper.player.setCooldown(it.type, value) }
        }

    val esper: Esper
        get() = psychic.esper

    open fun onInitialize() {}

    open fun onRegister() {}

    open fun onEnable() {}

    open fun onDisable() {}

    open fun test(): Boolean {
        return psychic.enabled && cooldownTick == 0 && psychic.mana >= spec.cost
    }

    fun checkState() {
        psychic.checkState()
    }

    internal fun createStatusText(): String? {
        val cooldownTick = this.cooldownTick

        if (cooldownTick > 0) {
            return "${ChatColor.AQUA}${ChatColor.BOLD}재사용 대기시간 ${ChatColor.RESET}${ChatColor.BOLD}${(cooldownTick / 2) / 10.0}"
        }

        return null
    }

}

abstract class CastableAbility : Ability() {

    var targeter: (() -> Any?)? = null

    override fun test(): Boolean {
        return psychic.channeling == null && super.test()
    }

    open fun tryCast(): Boolean {

        if (test()) {
            val targeter = this.targeter

            if (targeter != null) {
                targeter.invoke()?.let {
                    cast(spec.channelDuration, it)
                }
            } else {
                cast(spec.channelDuration)
            }

            return true
        }

        return false
    }

    protected fun cast(channelTicks: Int, target: Any? = null) {

        checkState()

        if (channelTicks > 0) {
            psychic.startChannel(this, channelTicks, target)
        } else {
            onCast(target)
        }
    }

    abstract fun onCast(target: Any?)

    open fun onInterrupt(target: Any?) {}

}

