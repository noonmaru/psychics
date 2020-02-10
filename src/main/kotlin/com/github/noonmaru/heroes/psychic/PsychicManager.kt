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
import com.github.noonmaru.heroes.loader.AbilityLoader
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableSortedMap
import java.io.File
import java.util.*

class PsychicManager(abilityFolder: File, psychicFolder: File) {

    private val loader = AbilityLoader()

    val abilities: Map<String, AbilityStatement>

    val psychics: Map<String, Psychic>

    init {
        abilityFolder.mkdirs()
        psychicFolder.mkdirs()

        abilities = abilityFolder.listFiles { _, name -> name.endsWith(".yml") }?.let { files ->
            val map = TreeMap<String, AbilityStatement>(String.CASE_INSENSITIVE_ORDER)

            for (file in files) {
                try {
                    val statement = loader.load(file)
                    val name = statement.description.name

                    Preconditions.checkArgument(name in map, "Duplicated ability name $name")

                    map[name] = statement
                } catch (e: Exception) {
                    HeroesLogger.warning("Failed to load Ability '${file.name}")
                    e.printStackTrace()
                }
            }

            ImmutableSortedMap.copyOf(map)
        } ?: ImmutableSortedMap.of()

        psychics = psychicFolder.listFiles { _, name -> name.endsWith(".yml") }?.let { files ->
            val map = TreeMap<String, Psychic>(String.CASE_INSENSITIVE_ORDER)

            for (file in files) {
                try {
                    val psychic = Psychic(this, file)
                    map[psychic.name] = psychic
                } catch (e: Exception) {
                    HeroesLogger.warning("Failed to load Psychic '${file.name}")
                    e.printStackTrace()
                }
            }

            ImmutableSortedMap.copyOf(map)
        } ?: ImmutableSortedMap.of()
    }
}