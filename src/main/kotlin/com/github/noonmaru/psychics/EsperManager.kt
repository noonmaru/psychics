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

package com.github.noonmaru.psychics

import com.github.noonmaru.psychics.plugin.PsychicPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.util.*

class EsperManager internal constructor(plugin: PsychicPlugin, private val dir: File) {

    private val espers = IdentityHashMap<Player, Esper>(Bukkit.getMaxPlayers())

    init {

        for (player in Bukkit.getOnlinePlayers()) {
            espers[player] = Esper(player)
        }

        plugin.server.pluginManager.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.LOWEST)
            fun onJoin(event: PlayerJoinEvent) {
                event.player.let { player ->
                    espers[player] = Esper(player)
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            fun onQuit(event: PlayerQuitEvent) {
                espers.remove(event.player)?.let { esper ->
                    esper.destroy()
                }
            }
        }, plugin)
    }

    fun getEsper(player: Player): Esper? {
        return espers[player]
    }

    fun getEspers(): Collection<Esper> {
        return espers.values
    }
}