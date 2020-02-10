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

package com.github.noonmaru.heroes

import com.github.noonmaru.heroes.psychic.HeroManager
import com.github.noonmaru.heroes.psychic.PsychicManager
import com.github.noonmaru.tap.event.EntityEventManager
import org.bukkit.entity.Player
import java.io.File

object Heroes {

    lateinit var entityEventManager: EntityEventManager
        private set

    lateinit var heroManager: HeroManager
        private set

    lateinit var psychicManager: PsychicManager
        private set

    internal fun initialize(plugin: HeroesPlugin) {
        val dataFolder = plugin.dataFolder
        dataFolder.mkdirs()

        entityEventManager = EntityEventManager(plugin)
        heroManager = HeroManager(File(dataFolder, "heroes"))
        psychicManager = PsychicManager(File(dataFolder, "abilities"), File(dataFolder, "psychics"))
    }
}

val Player.hero
    get() = Heroes.heroManager.heroes[this]

