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

package com.github.noonmaru.psychics.util

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.function.Predicate

class TargetFilter(
    private val player: Player,
    private val team: Team? = Bukkit.getScoreboardManager().mainScoreboard.getEntryTeam(player.name)
) : Predicate<Entity> {
    private var hostile = true

    override fun test(t: Entity): Boolean {
        if (t === player) return false

        if (t is LivingEntity) {
            if (t is Player) {
                val gameMode = t.gameMode

                if (gameMode == GameMode.SPECTATOR || gameMode == GameMode.CREATIVE) return false
            }

            val team = team ?: return hostile
            val name = if (t is Player) t.name else t.uniqueId.toString()
            val result = team.hasEntry(name)
            return if (hostile) !result else result
        }

        return false
    }

    fun friendly() {
        hostile = false
    }
}