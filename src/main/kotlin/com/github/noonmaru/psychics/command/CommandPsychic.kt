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

package com.github.noonmaru.psychics.command

import com.github.noonmaru.kommand.KommandBuilder
import com.github.noonmaru.kommand.KommandContext
import com.github.noonmaru.kommand.argument.KommandArgument
import com.github.noonmaru.kommand.argument.player
import com.github.noonmaru.kommand.sendFeedback
import com.github.noonmaru.psychics.PsychicConcept
import com.github.noonmaru.psychics.Psychics
import com.github.noonmaru.psychics.createTooltipBook
import com.github.noonmaru.psychics.esper
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal object CommandPsychic {
    fun register(builder: KommandBuilder) {
        builder.apply {
            then("attach") {
                then("player" to player()) {
                    then("psychic" to PsychicArgument) {
                        executes {
                            attach(it.sender, it.parseArgument("player"), it.parseArgument("psychic"))
                        }
                    }
                }
            }
            then("info") {
                then("psychic" to PsychicArgument) {
                    require { this is Player }
                    executes {
                        info(it.sender as Player, it.parseArgument("psychic"))
                    }
                }
            }
        }
    }

    private fun info(sender: Player, psychicConcept: PsychicConcept) {
        val tooltip = psychicConcept.createTooltipBook { sender.esper.getStatistic(it) }
        sender.inventory.addItem(tooltip)
    }

    private fun attach(sender: CommandSender, player: Player, psychicConcept: PsychicConcept) {
        player.esper.attachPsychic(psychicConcept).enabled = true
        sender.sendFeedback("${player.name}'s ability = ${psychicConcept.name}")
    }
}

object PsychicArgument : KommandArgument<PsychicConcept> {
    override fun parse(context: KommandContext, param: String): PsychicConcept? {
        return Psychics.psychicManager.getPsychicConcept(param)
    }

    override fun listSuggestion(context: KommandContext, target: String): Collection<String> {
        return Psychics.psychicManager.psychicConceptsByName.keys.asSequence().filter { it.startsWith(target, true) }
            .toList()
    }
}