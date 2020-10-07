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

import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.psychics.tooltip.addStats
import com.github.noonmaru.tap.config.*
import com.github.noonmaru.tap.template.renderTemplatesAll
import com.google.common.collect.ImmutableList
import net.md_5.bungee.api.ChatColor
import org.bukkit.boss.BarColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

@Name("common")
open class AbilityConcept {
    lateinit var name: String
        private set

    lateinit var container: AbilityContainer
        private set

    lateinit var psychicConcept: PsychicConcept
        private set

    lateinit var logger: Logger
        private set

    /**
     * 표시 이름 (I18N)
     */
    @Config(required = false)
    lateinit var displayName: String
        protected set

    /**
     * 능력의 타입
     * * PASSIVE - 자동으로 적용되는 능력
     * * ACTIVE - 직접 사용하는 능력
     * * TOGGLE - 활성/비활성 가능한 능력
     *
     * Ability의 기본값은 PASSIVE이며
     * ActiveAbility의 기본값은 ACTIVE입니다.
     */
    @Config
    var type = AbilityType.PASSIVE
        protected set

    /**
     * 필요 레벨
     */
    @Config(required = false)
    @RangeInt(min = 0)
    var levelRequirement = 0

    /**
     * 재사용 대기시간
     */
    @Config(required = false)
    @RangeInt(min = 0)
    var cooldownTicks = 0L
        protected set

    /**
     * 마나 소모
     */
    @Config(required = false)
    @RangeDouble(min = 0.0)
    var cost = 0.0
        protected set

    /**
     * 시전 시간
     */
    @Config(required = false)
    @RangeInt(min = 0)
    var castingTicks = 0L
        protected set

    /**
     * 시전 시간 -> 집중 시간
     * 스킬을 시전 시 외부에서 중단 가능
     */
    @Config(required = false)
    var interruptible = false
        protected set

    /**
     * 시전 상태 바 색상
     */
    @Config(required = false)
    var castingBarColor: BarColor? = null

    /**
     * 지속 시간
     */
    @Config(required = false)
    var durationTicks = 0L
        protected set

    /**
     * 사거리
     */
    @Config(required = false)
    @RangeDouble(min = 0.0)
    var range = 0.0
        protected set

    /**
     * 피해량
     */
    @Config(required = false)
    var damage: Damage? = null
        protected set

    /**
     * 치유량
     */
    @Config(required = false)
    var healing: EsperStatistic? = null
        protected set

    @Config("wand", required = false)
    private var _wand: ItemStack? = null

    internal val internalWand
        get() = _wand

    /**
     * 능력과 상호작용하는 [ItemStack]
     */
    var wand
        get() = _wand?.clone()
        protected set(value) {
            _wand = value?.clone()
        }

    @Config("supply-items")
    private var _supplyItems: List<ItemStack> = ImmutableList.of()

    /**
     * 기본 공급 아이템
     */
    var supplyItems: List<ItemStack>
        get() = _supplyItems.map { it.clone() }
        protected set(value) {
            _supplyItems = ImmutableList.copyOf(value.map { it.clone() })
        }

    /**
     * 능력의 설명
     * 템플릿을 사용 가능합니다.
     * * $variable - Config의 값 활용하기
     * * <variable> 능력 내부 템플릿 값 활용하기
     */
    @Config
    var description: List<String> = ArrayList(0)
        protected set

    internal fun initialize(
        name: String,
        container: AbilityContainer,
        psychicConcept: PsychicConcept,
        config: ConfigurationSection
    ): Boolean {
        this.name = name
        this.container = container
        this.psychicConcept = psychicConcept
        if (!this::displayName.isInitialized)
            this.displayName = container.description.name

        if (ActiveAbility::class.java.isAssignableFrom(container.abilityClass)) {
            type = AbilityType.ACTIVE
        }

        val ret = computeConfig(config, true)

        this.description = ImmutableList.copyOf(description.renderTemplatesAll(config))
        this._supplyItems = ImmutableList.copyOf(_supplyItems)

        return ret
    }

    internal fun renderTooltip(stats: (EsperStatistic) -> Double = { 0.0 }): TooltipBuilder {
        val tooltip = TooltipBuilder().apply {
            title = String.format("%s%s%-19s%s%19s", ChatColor.GOLD, ChatColor.BOLD, displayName, ChatColor.RESET, type)
            addStats(ChatColor.GREEN, "필요 레벨", levelRequirement, "레벨")
            addStats(ChatColor.AQUA, "재사용 대기시간", cooldownTicks / 20.0, "초")
            addStats(ChatColor.DARK_AQUA, "마나 소모", cost)
            addStats(ChatColor.BLUE, "${if (interruptible) "집중" else "시전"} 시간", castingTicks / 20.0, "초")
            addStats(ChatColor.DARK_GREEN, "지속 시간", durationTicks / 20.0, "초")
            addStats(ChatColor.LIGHT_PURPLE, "사거리", range, "블록")
            addStats(ChatColor.WHITE, "치유량", "<healing>", healing)
            addStats("damage", damage)
            addDescription(description)

            if (_supplyItems.isNotEmpty()) {
                addFooter(ChatColor.DARK_GREEN to "클릭하여 능력 아이템을 지급받으세요.")
            }

            addTemplates(
                "display-name" to displayName,
                "level-requirement" to levelRequirement,
                "cooldown-time" to cooldownTicks / 20.0,
                "cost" to cost,
                "casting-time" to castingTicks / 20.0,
                "range" to range,
                "duration" to durationTicks / 20.0
            )
            damage?.let { addTemplates("damage" to stats(it.stats)) }
            healing?.let { addTemplates("healing" to stats(it)) }
        }

        runCatching { onRenderTooltip(tooltip, stats) }

        return tooltip
    }

    internal fun createAbilityInstance(): Ability<*> {
        return container.abilityClass.getConstructor().newInstance().apply {
            initConcept(this@AbilityConcept)
        }
    }

    /**
     * 필드 변수 적용 후 호출
     */
    open fun onInitialize() {}

    /**
     * 툴팁 요청 시 호출
     */
    open fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {}
}