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

import com.github.noonmaru.tap.ref.UpstreamReference
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.function.Predicate

class Esper(p: Player) {
    val playerRef = UpstreamReference(p)

    val player: Player
        get() = playerRef.get()

    private val targetFilter: TargetFilter
        get() {
            val player = player
            val team = Bukkit.getScoreboardManager().mainScoreboard.getEntryTeam(player.name)

            return TargetFilter(player, team)
        }

    val hostileFilter: Predicate<Entity>
        get() = targetFilter

    val friendlyFilter: Predicate<Entity>
        get() = targetFilter.apply { setFriendly() }

    var psychic: Psychic? = null
        private set

    var valid = false

    fun applyPsychic(psychicSpec: PsychicSpec?): Psychic? {
        this.psychic?.unregister()

        val psychic = if (psychicSpec != null) Psychic(psychicSpec).apply { register(this@Esper) } else null

        this.psychic = psychic

        return psychic
    }

    internal fun destroy() {
        valid = false

        psychic?.unregister()
    }

    private class TargetFilter(
        private val player: Player,
        private val team: Team?
    ) : Predicate<Entity> {
        private var friendly = false

        internal fun setFriendly() {
            friendly = true
        }

        override fun test(entity: Entity): Boolean {
            if (entity is LivingEntity) {
                val player = this.player

                if (entity === player) return false
                if (entity === player.vehicle) return false
                if (entity.vehicle === player) return false

                var isFriendlyEntity = false

                if (entity is Player) {
                    val team = team

                    if (team != null && team.hasEntry(entity.name))
                        isFriendlyEntity = true
                }

                if (friendly) return isFriendlyEntity

                return !isFriendlyEntity
            }

            return false
        }
    }
}

