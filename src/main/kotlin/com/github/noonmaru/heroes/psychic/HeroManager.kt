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

package com.github.noonmaru.heroes.psychic

import com.github.noonmaru.heroes.HeroesPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.util.*

class HeroManager internal constructor(internal val heroesFolder: File) {

    private val _heroes = IdentityHashMap<Player, Hero>()

    val heroes: Map<Player, Hero>
        get() = _heroes

    init {
        for (player in Bukkit.getOnlinePlayers()) {
            _heroes[player] = Hero(player)
        }

        Bukkit.getServer().pluginManager.apply {
            registerEvents(HeroListener(), HeroesPlugin.instance)
        }
    }

    inner class HeroListener : Listener {
        @EventHandler
        fun onJoin(event: PlayerJoinEvent) {
            event.player.let { player ->
                _heroes[player] = Hero(player)
            }
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            _heroes.remove(event.player)?.psychic.let {
                it.enabled = false
                it.unregister()
            }
        }
    }
}



