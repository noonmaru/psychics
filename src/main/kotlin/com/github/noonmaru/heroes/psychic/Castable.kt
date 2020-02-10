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

import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Entity
import org.bukkit.util.RayTraceResult

interface Castable {
    fun onCast(heroAbility: HeroAbility): Boolean
}

interface TargetCastable : Castable {

    var range: Double

    var raySize: Double

    var filter: (Entity) -> Boolean

    override fun onCast(heroAbility: HeroAbility): Boolean {
        val player = heroAbility.hero.player
        val world = player.world

        val start = player.eyeLocation
        val direction = start.direction

        return world.rayTrace(start, direction, range, FluidCollisionMode.NEVER, true, raySize, filter)?.let { result ->
            onCast(heroAbility, result)
        } ?: false
    }

    fun onCast(heroAbility: HeroAbility, result: RayTraceResult): Boolean

}