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

import com.github.noonmaru.psychics.util.Tick
import com.github.noonmaru.tap.ref.UpstreamReference
import kotlin.math.max

abstract class Ability<T : AbilityConcept> {

    lateinit var concept: T
        private set

    var cooldownTicks: Long = 0
        get() {
            return max(0, field - Tick.currentTicks)
        }
        set(value) {
            field = Tick.currentTicks + max(0L, value)
        }

    private lateinit var psychicRef: UpstreamReference<Psychic>

    val psychic: Psychic
        get() = psychicRef.get()

    val esper
        get() = psychic.esper

    @Suppress("UNCHECKED_CAST")
    internal fun initConcept(concept: AbilityConcept) {
        this.concept = concept as T
    }

    internal fun initPsychic(psychic: Psychic) {
        this.psychicRef = UpstreamReference(psychic)
    }

    open fun test(): Boolean {
        val psychic = psychic

        return psychic.enabled && cooldownTicks == 0L && psychic.mana >= concept.cost
    }

    /**
     * 초기화 후 호출됩니다.
     */
    open fun onInitialize() {}

    /**
     * 플레이어에게 적용 후 호출됩니다.
     */
    open fun onAttach() {}

    /**
     * 플레이어로부터 해제 후 호출됩니다.
     */
    open fun onDetach() {}

    /**
     * 정보를 디스크에 저장 할 때 호출됩니다.
     */
    open fun onSave() {}

    /**
     * 정보를 디스크로부터 불러 올 때 호출됩니다.
     */
    open fun onLoad() {}

    /**
     * 능력이 활성화 될 때 호출됩니다.
     */
    open fun onEnable() {}

    /**
     * 능력이 비활성화 될 때 호출됩니다.
     */
    open fun onDisable() {}

    fun checkState() {
        psychic.checkState()
    }

    fun checkEnabled() {
        psychic.checkEnabled()
    }
}

abstract class ActiveAbility<T : AbilityConcept> : Ability<T>() {
    var targeter: (() -> Any?)? = null

    override fun test(): Boolean {
        return psychic.channeling == null && super.test()
    }

    open fun tryCast(castingTicks: Int = concept.castingTicks, targeter: (() -> Any?)? = null): Boolean {
        if (test()) {
            if (targeter != null) {
                val target = targeter.invoke() ?: return false

                cast(castingTicks, target)
            } else {
                cast(castingTicks)
            }

            return true
        }

        return false
    }

    protected fun cast(castingTicks: Int, target: Any? = null) {
        checkState()

        if (castingTicks > 0) {
            psychic.startChannel(this, castingTicks, target)
        } else {
            onCast(target)
        }
    }

    abstract fun onCast(target: Any?)

    open fun onInterrupt(target: Any?) {}
}