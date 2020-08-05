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

package com.github.noonmaru.psychics.attribute

import com.google.common.collect.ImmutableSortedMap
import net.md_5.bungee.api.ChatColor
import java.util.*

enum class EsperAttribute(
    val abbr: String,
    val i18DisplayName: String,
    val color: ChatColor
) {
    ATTACK_DAMAGE("ATK", "공격력", ChatColor.GOLD),
    LEVEL("LVL", "레벨", ChatColor.GREEN),
    DEFENSE("DEF", "방어", ChatColor.WHITE),
    HEALTH("HP", "체력", ChatColor.RED),
    MANA("MP", "마나", ChatColor.AQUA);

    companion object {
        val byAbbr: Map<String, EsperAttribute>

        init {
            val map = TreeMap<String, EsperAttribute>(String.CASE_INSENSITIVE_ORDER)

            for (value in values()) {
                map[value.abbr] = value
            }

            byAbbr = ImmutableSortedMap.copyOf(map)
        }
    }
}