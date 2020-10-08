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
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.item.removeAllPsychicbounds
import com.github.noonmaru.tap.fake.FakeEntityServer
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class EventListener(
    val psychicManager: PsychicManager,
    val fakeEntityServer: FakeEntityServer
) : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        psychicManager.addPlayer(player)
        fakeEntityServer.addPlayer(player)

        if (player.esper.psychic == null) {
            player.inventory.removeAllPsychicbounds()
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        psychicManager.removePlayer(player)
        fakeEntityServer.removePlayer(player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val action = event.action
        val hand = event.hand
        val item = event.item

        if (action != Action.PHYSICAL && hand == EquipmentSlot.HAND && item != null) {
            val player = event.player
            val wandAction =
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) ActiveAbility.WandAction.LEFT_CLICK
                else ActiveAbility.WandAction.RIGHT_CLICK

            player.esper.psychic?.castByWand(event, wandAction, item)
        }
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (item.type != Material.AIR) {
            player.esper.psychic?.castByWand(event, ActiveAbility.WandAction.RIGHT_CLICK, item)
        }
    }

    //psychicbound
    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked.gameMode == GameMode.CREATIVE) return

        val type = event.inventory.type

        if (type != InventoryType.CRAFTING) {
            event.currentItem?.let { item ->
                if (item.isPsychicbound) {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (event.entity.itemStack.isPsychicbound) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop
        if (item.itemStack.isPsychicbound) {
            if (event.player.isSneaking) {
                item.remove()
            } else {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val entity = event.entity

        if (entity is Player && entity.killer != null) {
            event.isCancelled = true
        }
    }

    private val Player.esper: Esper
        get() = requireNotNull(psychicManager.getEsper(this))
}

private fun Psychic.castByWand(event: PlayerEvent, action: ActiveAbility.WandAction, item: ItemStack) {
    esper.psychic?.let { psychic ->
        val ability = psychic.getAbilityByWand(item)

        if (ability is ActiveAbility) {
            val result = ability.tryCast(event, action)

            if (result !== TestResult.SUCCESS) {
                esper.player.sendActionBar(result.getMessage(ability))
            }
        }
    }
}