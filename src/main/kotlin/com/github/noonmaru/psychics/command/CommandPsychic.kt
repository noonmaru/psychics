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
import com.github.noonmaru.kommand.argument.suggestions
import com.github.noonmaru.kommand.sendFeedback
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.PsychicConcept
import com.github.noonmaru.psychics.PsychicManager
import com.github.noonmaru.psychics.createTooltipBook
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal object CommandPsychic {
    internal lateinit var manager: PsychicManager

    internal fun initModule(manager: PsychicManager) {
        this.manager = manager
    }

    fun register(builder: KommandBuilder) {
        builder.apply {
            then("attach") {
                then("player" to player()) {
                    then("psychic" to PsychicConceptArgument) {
                        executes {
                            attach(it.sender, it.parseArgument("player"), it.parseArgument("psychic"))
                        }
                    }
                }
            }
            then("info") {
                then("psychic" to PsychicConceptArgument) {
                    require { this is Player }
                    executes {
                        info(it.sender as Player, it.parseArgument("psychic"))
                    }
                }
            }
            then("supply") {
                then("ability" to AbilityConceptArgument) {
                    require(this is Player)
                    executes {
                        supply(it.sender as Player, it.parseArgument("ability"))
                    }
                }
            }
        }
    }

    private fun info(sender: Player, psychicConcept: PsychicConcept) {
        val tooltip = psychicConcept.createTooltipBook { requireNotNull(manager.getEsper(sender)).getStatistic(it) }
        sender.inventory.addItem(tooltip)
    }

    private fun attach(sender: CommandSender, player: Player, psychicConcept: PsychicConcept) {
        requireNotNull(manager.getEsper(player)).attachPsychic(psychicConcept).enabled = true
        sender.sendFeedback("${player.name}'s ability = ${psychicConcept.name}")
    }

    private fun supply(sender: Player, abilityConcept: AbilityConcept) {
        val inv = sender.inventory

        for (item in abilityConcept.supplyItems()) {
            inv.addItem(item)
        }
    }
}

object PsychicConceptArgument : KommandArgument<PsychicConcept> {
    override fun parse(context: KommandContext, param: String): PsychicConcept? {
        return CommandPsychic.manager.getPsychicConcept(param)
    }

    override fun listSuggestion(context: KommandContext, target: String): Collection<String> {
        return CommandPsychic.manager.psychicConceptsByName.keys.asSequence().filter { it.startsWith(target, true) }
            .toList()
    }
}

object AbilityConceptArgument : KommandArgument<AbilityConcept> {
    override fun parse(context: KommandContext, param: String): AbilityConcept? {
        val sender = context.sender

        if (sender is Player) {
            return CommandPsychic.manager.getEsper(sender)?.psychic?.concept?.abilityConcepts?.find { it.name == param }
        }

        return null
    }

    override fun listSuggestion(context: KommandContext, target: String): Collection<String> {
        val sender = context.sender

        if (sender is Player) {
            return CommandPsychic.manager.getEsper(sender)?.psychic?.concept?.abilityConcepts?.suggestions(target) { it.name }
                ?: emptyList()
        }

        return emptyList()
    }
}