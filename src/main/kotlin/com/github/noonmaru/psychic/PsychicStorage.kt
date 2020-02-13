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

package com.github.noonmaru.psychic

import com.github.noonmaru.psychic.loader.AbilityLoader
import com.google.common.collect.ImmutableSortedMap
import java.io.File
import java.util.*

class PsychicStorage internal constructor(abilityModelsDir: File, psychicsDir: File) {

    val abilityLoader = AbilityLoader()

    val abilityModels: Map<String, AbilityModel>

    val psychicSpecs: Map<String, PsychicSpec>

    init {
        abilityModelsDir.mkdirs()
        psychicsDir.mkdirs()

        abilityModels = abilityModelsDir.listFiles { _, name -> name.endsWith(".jar") }.let { files ->
            val models = TreeMap<String, AbilityModel>(String.CASE_INSENSITIVE_ORDER)

            files?.forEach { file ->
                runCatching {
                    val model = abilityLoader.load(file)
                    models[model.description.name] = model
                }.onFailure {
                    Psychics.logger.warning("Failed to load AbilityModel for '${file.name}")
                    it.printStackTrace()
                }
            }

            ImmutableSortedMap.copyOfSorted(models)
        }

        psychicSpecs = psychicsDir.listFiles { _, name -> name.endsWith(".yml") }.let { files ->
            val specs = TreeMap<String, PsychicSpec>(String.CASE_INSENSITIVE_ORDER)

            files?.forEach { file ->
                kotlin.runCatching {
                    val spec = PsychicSpec(this, file)
                    specs[spec.name] = spec
                }.onFailure {
                    Psychics.logger.info("Failed to load Psychic for $file")
                    it.printStackTrace()
                }
            }

            ImmutableSortedMap.copyOfSorted(specs)
        }
    }

}