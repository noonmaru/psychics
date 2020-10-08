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

package com.github.noonmaru.psychics.damage

import com.github.noonmaru.psychics.attribute.EsperStatistic
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.serialization.ConfigurationSerializable

class Damage(
    val type: DamageType,
    val stats: EsperStatistic
) : ConfigurationSerializable {
    override fun toString(): String {
        return "$stats${ChatColor.BOLD}${type.i18Name}"
    }

    override fun serialize(): Map<String, Any> {
        return mapOf(
            TYPE to type.name,
            STATS to stats.serialize()
        )
    }

    companion object {
        private const val TYPE = "type"
        private const val STATS = "stats"

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun deserialize(map: Map<String, *>): Damage {
            val type = DamageType.valueOf(requireNotNull(map[TYPE]) as String)

            val statsValue = requireNotNull((map[STATS] as ConfigurationSection).getValues(false))
            val stats = EsperStatistic.deserialize(statsValue)

            return Damage(type, stats)
        }
    }
}