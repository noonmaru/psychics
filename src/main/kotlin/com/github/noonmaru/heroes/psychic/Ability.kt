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

import com.github.noonmaru.tap.config.Config
import org.bukkit.inventory.ItemStack

abstract class Ability<T : HeroAbility> {

    lateinit var statement: AbilityStatement

    @Config
    var displayName: String = "undefined"
        protected set

    @Config
    var type: Type = Type.PASSIVE
        protected set

    @Config(required = false)
    var cooldown: Int = 0
        protected set

    @Config(required = false)
    var cost: Int = 0
        protected set

    @Config(required = false)
    var castTime: Int = 0
        protected set

    @Config(required = false)
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
    var description: List<String> = listOf("설명이 없습니다.")
        protected set

    /**
     * [Hero]가 [Psychic]을 부여받을때 호출됩니다.
     * 이 함수 내에서 [HeroPsychic.registerEvents]를 사용하는 것을 추천합니다.
     */
    var onRegister: ((hero: Hero, psychic: HeroPsychic, heroAbility: T) -> Unit)? = null
        protected set

    /**
     * [Hero]가 [Psychic]을 잃을때 호출됩니다.
     */
    var onUnregister: ((hero: Hero, psychic: HeroPsychic, heroAbility: T) -> Unit)? = null
        protected set

    /**
     * [Hero]의 [HeroPsychic]이 활성화 될 때 호출됩니다.
     */
    var onEnable: ((hero: Hero, psychic: HeroPsychic, heroAbility: T) -> Unit)? = null
        protected set

    /**
     * [Hero]의 [HeroPsychic]이 비활성화 될 때 호출됩니다.
     */
    var onDisable: ((hero: Hero, psychic: HeroPsychic, heroAbility: T) -> Unit)? = null
        protected set

    @Suppress("UNCHECKED_CAST")
    internal fun invoke(
        function: (hero: Hero, psychic: HeroPsychic, heroAbility:) -> Unit,
        hero: Hero,
        psychic: HeroPsychic,
        heroAbility: HeroAbility
    ) {
        function.invoke(hero, psychic, heroAbility as T)
    }

    /**
     * 객체 생성되고 설정이 적용 된 후 호출됩니다.
     */
    open fun onInitialize() {}

    /**
     * [Psychic]이 새로 생성될 때 호출됩니다.
     * 추가 데이터가 필요할 경우 [HeroAbility]를 확장한 클래스의 인스턴스를 반환해주세요
     */
    abstract fun createHeroAbility(): T

    enum class Type(val displayName: String) {
        MOVEMENT("이동"),
        CASTING("시전"),
        SPELL("주문"),
        PASSIVE("기본지속")
    }


}