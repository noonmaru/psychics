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

import com.github.noonmaru.psychics.*
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class EventListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        Psychics.psychicManager.addPlayer(player)
        Psychics.fakeEntityServer.addPlayer(player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        Psychics.psychicManager.removePlayer(player)
        Psychics.fakeEntityServer.removePlayer(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val action = event.action
        val hand = event.hand
        val item = event.item

        if (action != Action.PHYSICAL && hand == EquipmentSlot.HAND && item != null) {
            val player = event.player
            player.esper.psychic?.castByWand(item)
        }
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (item.type != Material.AIR) {
            player.esper.psychic?.castByWand(item)
        }
    }
}

private fun Psychic.castByWand(item: ItemStack) {
    esper.psychic?.let { psychic ->
        val ability = psychic.getAbilityByWand(item)

        if (ability is ActiveAbility) {
            val result = ability.tryCast()

            if (result !== TestResult.SUCCESS) {
                esper.player.sendActionBar(result.getMessage(ability))
            }
        }
    }
}