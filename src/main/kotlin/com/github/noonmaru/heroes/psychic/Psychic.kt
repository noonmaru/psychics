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

import com.github.noonmaru.heroes.HeroesLogger
import com.github.noonmaru.tap.config.applyConfig
import com.google.common.collect.ImmutableList
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class Psychic internal constructor(val manager: PsychicManager, val file: File) {

    val name: String

    val displayName: String

    val mana: Int

    val manaRegen: Int

    val manaRegenTick: Int

    val description: List<String>

    val abilities: List<Ability<*>>

    init {
        val config = YamlConfiguration.loadConfiguration(file)

        name = file.name.removeSuffix(".yml")
        displayName = config.getString("display-name") ?: name
        mana = config.getInt("mana")
        manaRegen = config.getInt("mana-regen")
        manaRegenTick = config.getInt("mana-regen-tick")
        description = ImmutableList.copyOf(config.getStringList("description"))
        val abilities = config.getConfigurationSection("abilities")?.let { section ->

            var absent = false
            val list = ArrayList<Ability<*>>()

            for ((abilityName, value) in section.getValues(false)) {
                if (value !is ConfigurationSection) continue

                val statement = manager.abilities[abilityName]

                if (statement != null) {
                    try {
                        list += statement.abilityClass.newInstance().apply {
                            if (applyConfig(value, true))
                                absent = true
                            _wand?.amount = 1
                            onInitialize()
                        }
                    } catch (e: Exception) {
                        HeroesLogger.warning("Failed to initialize '$abilityName")
                        e.printStackTrace()
                    }
                }

                HeroesLogger.warning("Not found ability for '$abilityName'")
            }

            if (absent)
                config.save(file)

            for ((item, abilities) in list.groupBy { it.wand }) {
                if (item != null && abilities.isNotEmpty()) {
                    throw IllegalArgumentException("Duplicated wand $item ${abilities.joinToString(transform = { it.statement.description.name })}")
                }
            }

            ImmutableList.copyOf(list)
        } ?: ImmutableList.of()

        this.abilities = abilities
    }

}