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
import com.github.noonmaru.psychics.*
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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
                require { this is Player && requireNotNull(manager.getEsper(this)).psychic != null }
                executes {
                    val sender = it.sender as Player
                    info(sender, manager.getEsper(sender)!!.psychic!!.concept)
                }
            }
            then("supply") {
                require { this is Player }
                executes {
                    supply(it.sender as Player)
                }
                then("ability" to AbilityConceptArgument) {
                    require { this is Player }
                    executes {
                        supply(it.sender as Player, it.parseArgument("ability"))
                    }
                }
            }
        }
    }

    private fun info(sender: Player, psychicConcept: PsychicConcept) {
        val inv = sender.inventory

        for (itemStack in inv) {
            if (itemStack != null && itemStack.type == Material.WRITTEN_BOOK && itemStack.isPsychicWrittenBook) {
                psychicConcept.updateTooltipBook(itemStack, requireNotNull(manager.getEsper(sender))::getStatistic)
                return
            }
        }

        val tooltip = psychicConcept.createTooltipBook { requireNotNull(manager.getEsper(sender)).getStatistic(it) }
        inv.addItem(tooltip)
    }

    private fun attach(sender: CommandSender, player: Player, psychicConcept: PsychicConcept) {
        requireNotNull(manager.getEsper(player)).attachPsychic(psychicConcept).enabled = true
        sender.sendFeedback("${player.name}'s ability = ${psychicConcept.name}")
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
        val inv = sender.inventory
        val items = abilityConcept.supplyItems()

        out@ for (item in items) {
            for (invItem in inv) {
                if (invItem == null)
                    continue
                if (invItem.isSimilarLore(item))
                    continue@out
            }

            inv.addItem(item)
        }
    }
}

private fun ItemStack.isSimilarLore(other: ItemStack): Boolean {
    val meta = itemMeta
    val otherMeta = other.itemMeta

    return type == other.type && data == other.data && meta.displayName == itemMeta.displayName && meta.lore == otherMeta.lore
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