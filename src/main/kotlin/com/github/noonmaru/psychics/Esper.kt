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

import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

class Esper(
    player: Player
) {
    val player: Player
        get() = requireNotNull(playerRef.get()) { "Cannot get reference as it has already been Garbage Collected" }

    private val playerRef = WeakReference(player)

    var psychic: Psychic? = null
        private set

    val isOnline
        get() = playerRef.get() != null

    fun getAttribute(attr: EsperAttribute): Double {
        return when (attr) {
            EsperAttribute.ATTACK_DAMAGE -> TODO()
            EsperAttribute.LEVEL -> player.level.toDouble()
            EsperAttribute.DEFENSE -> player.getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0
            EsperAttribute.HEALTH -> player.health
            EsperAttribute.MANA -> psychic?.mana ?: 0.0
        }
    }

    fun getStatistic(stats: EsperStatistic): Double {
        var ret = 0.0

        for ((attr, ratio) in stats.stats) {
            val value = getAttribute(attr)

            ret += value * ratio
        }

        return ret
    }

    fun attachPsychic(concept: PsychicConcept) {
        detachPsychic()

        this.psychic = concept.createInstance().apply {
            attach(this@Esper)
        }
    }

    fun detachPsychic() {
        psychic?.let { psychic ->
            this.psychic = null
            psychic.destroy()
        }
    }
}