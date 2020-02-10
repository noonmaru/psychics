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

import com.github.noonmaru.heroes.utils.currentTicks
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max

open class HeroAbility {
    lateinit var ability: Ability<out HeroAbility>
        internal set

    lateinit var heroPsychic: HeroPsychic
        internal set

    val hero: Hero
        get() = heroPsychic.hero

    /**
     * 능력의 재사용 대기시간입니다.
     *
     * 시간의 기준은 tick(50ms)입니다
     *
     * 단순히 대입하면 설정, 업데이트됩니다.
     */
    var cooldown: Int = 0
        get() = max(currentTicks() - field, 0)
        set(value) {
            field = currentTicks() + value

            ability._wand?.let { wand ->
                hero.player.setCooldown(wand.type, value) // 클라이언트 재사용 대기시간 패킷
            }
        }

    private var lockdowns: MutableSet<Any> = Collections.emptySet()

    val isLockdown
        get() = lockdowns.isNotEmpty()

    /**
     * 활성화 여부를 반환합니다.
     * 결과값은 [HeroPsychic]의 활성화 여부, 락다운등에 영향을 받습니다.
     *
     * **true** = 활성화
     *
     * **false** = 비활성화
     *
     */
    val enabled
        get() = heroPsychic.enabled && lockdowns.isEmpty()

    internal fun initialize(ability: Ability<*>, heroPsychic: HeroPsychic) {
        this.ability = ability
        this.heroPsychic = heroPsychic
    }

    /**
     * 능력을 사용 할 수 있는지 확인합니다.
     *
     * 모든 요건 (상태, 락다운, 재사용대기시간, 마나 비용)이 충족되면 true를 반환합니다.
     *
     * @return result 조건 충족
     */
    fun test(): Boolean {
        return heroPsychic.valid && enabled && cooldown <= 0 && heroPsychic.mana > ability.cost
    }

    /**
     * [HeroAbility]의 락다운을 설정합니다.
     *
     * 락다운이 걸릴 때 [Ability.onEnable]이 호출됩니다.
     *
     * 락다운이 풀릴 때 [Ability.onDisable]이 호출됩니다.
     *
     *
     * @param contributor 락다운의 제공자입니다.
     *
     * _예) 이동금지, 침묵, 기절, 능력금지구역 등
     *
     * @param lockdown 락다운 설정/해제
     *
     * @return result 영향 여부
     */
    fun setLockdown(contributor: Any, lockdown: Boolean): Boolean {
        val prevCondition = isLockdown
        val result = if (lockdown) {
            if (lockdowns == Collections.EMPTY_LIST)
                lockdowns = HashSet() //새로운 요소가 추가될때 Collection.EMPTY_SET -> HashSet으로 변경
            lockdowns.add(contributor)
        } else {
            lockdowns.remove(contributor)
        }

        if (heroPsychic.enabled && result) {
            val currentCondition = isLockdown
            if (prevCondition != currentCondition) { // 이전 상태와 다를때 함수 적용
                if (currentCondition)
                    ability.onRegister.in
                else
                    ability.onDisable(hero, heroPsychic, this)
            }
        }

        return result
    }

    /**
     * [HeroPsychic]의 상태를 확인합니다.
     *
     * [HeroPsychic.valid]가 **false**인 경우 [IllegalStateException]을 발생시킵니다.
     *
     * @exception IllegalArgumentException
     */
    fun checkState() {
        heroPsychic.checkState()
    }
}