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

package com.github.noonmaru.psychic.plugin

import com.github.noonmaru.psychic.Psychics
import com.github.noonmaru.psychic.cmd.CommandApply
import com.github.noonmaru.tap.command.CommandManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Noonmaru
 */
class PsychicPlugin : JavaPlugin() {

    companion object {
        @JvmStatic
        lateinit var instance: PsychicPlugin
            private set
    }

    override fun onEnable() {
        instance = this

        Psychics.initialize(this)
        setupCommands()

        Psychics.storage.abilityModels.values.let { models ->
            logger.info("Loaded abilities: ${models.joinToString { model -> model.description.name }}")
        }

        Psychics.storage.psychicSpecs.values.let { specs ->
            logger.info("Loaded psychics: ${specs.joinToString { spec -> spec.name }}")
        }

        logger.info("HEROES ASSEMBLE!")
    }

    private fun setupCommands() {
        CommandManager().apply {
            addCommand("apply", CommandApply()).apply {
                usage = "[Player] <Psychic>"
                description = "능력을 적용합니다."
            }
        }.let { command ->
            getCommand("psychic")?.apply {
                setExecutor(command)
                tabCompleter = command
            }
        }
    }
}
