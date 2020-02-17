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

package com.github.noonmaru.psychics

import com.github.noonmaru.psychics.utils.processTemplatesAll
import com.github.noonmaru.tap.config.applyConfig
import com.google.common.collect.ImmutableList
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.math.max

class PsychicSpec(storage: PsychicStorage, specFile: File) {

    val name: String

    val displayName: String

    val mana: Double

    val manaRegenPerSec: Double

    val description: List<String>

    val abilities: List<AbilitySpec>

    init {
        val config = YamlConfiguration.loadConfiguration(specFile)

        name = specFile.name.removeSuffix(".yml")
        displayName = config.getString("display-name") ?: name
        mana = max(config.getDouble("mana"), 0.0)
        manaRegenPerSec = config.getDouble("mana-regen-per-sec")
        description = ImmutableList.copyOf(config.getStringList("description").processTemplatesAll(config))

        abilities = config.getConfigurationSection("abilities")?.run {
            val list = ArrayList<AbilitySpec>()

            var absent = false

            for ((abilityName, value) in getValues(false)) {
                if (value is ConfigurationSection) {
                    storage.abilityModels[abilityName]?.let { abilityModel ->
                        list += abilityModel.specClass.newInstance().apply {
                            if (applyConfig(value, true)) {
                                absent = true
                            }
                            initialize(abilityModel, this@PsychicSpec, value)
                            onInitialize()
                        }

                    } ?: throw NullPointerException("Not found AbilityModel for '$abilityName'")
                }
            }

            if (absent)
                config.save(specFile)

            ImmutableList.copyOf(list)

        } ?: ImmutableList.of()
    }
}