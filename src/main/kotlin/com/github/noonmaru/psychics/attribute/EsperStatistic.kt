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

import com.github.noonmaru.psychics.format.decimalFormat
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.util.*
import kotlin.math.exp
import kotlin.math.max

class EsperStatistic internal constructor(pairs: List<Pair<EsperAttribute, Double>>) : ConfigurationSerializable {
    internal val stats: Map<EsperAttribute, Double>

    init {
        val stats = EnumMap<EsperAttribute, Double>(EsperAttribute::class.java)
        for (pair in pairs) {
            stats[pair.first] = pair.second
        }
        this.stats = stats
    }

    override fun toString(): String {
        val builder = StringBuilder()

        for ((attr, ratio) in stats) {
            builder
                .append(attr.color)
                .append('(').append(ratio.decimalFormat()).append(' ')
                .append(attr.abbr).append(')')
                .append(ChatColor.RESET)
        }

        return builder.toString()
    }

    override fun serialize(): Map<String, Any> {
        return stats.mapKeys { it.key.abbr }
    }

    companion object {
        init {
            ConfigurationSerialization.registerClass(EsperStatistic::class.java)
        }

        @JvmStatic
        fun deserialize(map: Map<String, *>): EsperStatistic {
            val list = ArrayList<Pair<EsperAttribute, Double>>()

            for ((abbr, value) in map) {
                val attribute = requireNotNull(EsperAttribute.byAbbr[abbr])
                val ratio = requireNotNull(value) as Double

                list += attribute to ratio
            }

            return EsperStatistic(list)
        }

        fun of(vararg pairs: Pair<EsperAttribute, Double>): EsperStatistic {
            return EsperStatistic(pairs.asList())
        }

        fun of(pairs: List<Pair<EsperAttribute, Double>>): EsperStatistic {
            return EsperStatistic(pairs)
        }

        /**
         * 레벨별 데미지를 반환합니다.
         *
         * 40레벨 기준 25의 피해
         *
         * 최대 26.8의 피해
         *
         * [sigmoid function](https://www.desmos.com/calculator/kn9tpwdan5)
         */
        @JvmStatic
        fun calculateAttachDamageByLevel(level: Int): Double {
            //https://www.desmos.com/calculator/kn9tpwdan5
            val b = -0.083
            val k = 25.8

            return 1.0 + (k * 2.0 / (1.0 + exp(max(0, level) * b))) - k
        }
    }

}