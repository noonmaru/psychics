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

import com.github.noonmaru.invfx.openWindow
import com.github.noonmaru.kommand.KommandBuilder
import com.github.noonmaru.kommand.KommandContext
import com.github.noonmaru.kommand.argument.KommandArgument
import com.github.noonmaru.kommand.argument.player
import com.github.noonmaru.kommand.argument.playerTarget
import com.github.noonmaru.kommand.argument.suggestions
import com.github.noonmaru.kommand.sendFeedback
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.PsychicConcept
import com.github.noonmaru.psychics.PsychicManager
import com.github.noonmaru.psychics.esper
import com.github.noonmaru.psychics.invfx.InvPsychic
import com.github.noonmaru.psychics.item.addItemNonDuplicate
import com.github.noonmaru.psychics.plugin.PsychicPlugin
import com.github.noonmaru.tap.util.updateFromGitHubMagically
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal object CommandPsychic {
    private lateinit var plugin: PsychicPlugin
    lateinit var manager: PsychicManager

    internal fun initModule(plugin: PsychicPlugin, manager: PsychicManager) {
        this.plugin = plugin
        this.manager = manager
    }

    fun register(builder: KommandBuilder) {
        builder.apply {
            then("update") {
                executes {
                    plugin.updateFromGitHubMagically("noonmaru", "psychics", "Psychics.jar", it.sender::sendMessage)
                }
            }
            then("attach") {
                then("players" to playerTarget()) {
                    then("psychic" to PsychicConceptArgument) {
                        executes {
                            attach(it.sender, it.parseArgument("players"), it.parseArgument("psychic"))
                        }
                    }
                }
            }
            then("detach") {
                then("player" to player()) {
                    executes {
                        detach(it.sender, it.parseArgument("player"))
                    }
                }
            }
            then("info") {
                require { this is Player }
                then("psychic" to PsychicConceptArgument) {
                    executes {
                        info(it.sender as Player, it.parseArgument("psychic"))
                    }
                }
                executes {
                    val sender = it.sender as Player
                    val psychic = manager.getEsper(sender)?.psychic

                    if (psychic == null) sender.sendMessage("능력이 없습니다.")
                    else info(sender, psychic.concept)
                }
            }
            then("supply") {
                require { this is Player }
                executes {
                    supply(it.sender as Player)
                }
                then("ability" to AbilityConceptArgument) {
                    executes {
                        supply(it.sender as Player, it.parseArgument("ability"))
                    }
                }
            }
            then("reload") {
                executes {
                    plugin.reloadPsychics()
                    Bukkit.broadcast("${ChatColor.GREEN}Psychics reload complete.", "psychics.reload")
                }
            }
        }
    }

    private fun info(sender: Player, psychicConcept: PsychicConcept) {
        sender.openWindow(InvPsychic.create(psychicConcept, sender.esper::getStatistic))
    }

    private fun attach(sender: CommandSender, players: List<Player>, psychicConcept: PsychicConcept) {
        for (player in players) {
            requireNotNull(manager.getEsper(player)).attachPsychic(psychicConcept).isEnabled = true
            sender.sendFeedback("${player.name}'s ability = ${psychicConcept.name}")
        }
    }

    private fun detach(sender: CommandSender, player: Player) {
        player.esper.detachPsychic()
        sender.sendFeedback("${player.name}'s ability = NONE")
    }

    private fun supply(sender: Player) {
        val esper = requireNotNull(manager.getEsper(sender))
        esper.psychic?.let {
            for (abilityConcept in it.concept.abilityConcepts) {
                supply(sender, abilityConcept)
            }
        }
    }

    private fun supply(sender: Player, abilityConcept: AbilityConcept) {
        sender.inventory.addItemNonDuplicate(abilityConcept.supplyItems)
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