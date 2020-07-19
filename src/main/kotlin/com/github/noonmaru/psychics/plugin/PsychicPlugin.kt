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

package com.github.noonmaru.psychics.plugin

import com.github.noonmaru.psychics.Psychics
import com.github.noonmaru.psychics.cmd.CommandApply
import com.github.noonmaru.psychics.cmd.CommandInfo
import com.github.noonmaru.tap.command.command
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
        command("psychics") {
            help("help")
            component("info") {
                usage = "[Psychic]"
                description = "능력 정보를 확인합니다."
                CommandInfo()
            }
            component("apply") {
                usage = "<Psychic> [Player...]"
                description = "능력을 적용합니다."
                CommandApply()
            }
        }
    }

    override fun onDisable() {
        for (esper in Psychics.esperManager.getEspers()) {
            esper.destroy()
        }

        Psychics.fakeEntityServer.shutdown()
    }
}
