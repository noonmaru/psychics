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

package com.github.noonmaru.psychic.cmd

import com.github.noonmaru.psychic.Psychics
import com.github.noonmaru.psychic.esper
import com.github.noonmaru.tap.command.ArgumentList
import com.github.noonmaru.tap.command.CommandComponent
import com.github.noonmaru.tap.command.tabComplete
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class CommandApply : CommandComponent {

    override val argsCount: Int
        get() = 1

    override fun onCommand(sender: CommandSender, label: String, componentLabel: String, args: ArgumentList): Boolean {
        val psychicName = args.next()
        Psychics.storage.psychicSpecs[psychicName]?.let { psychic ->
            val esper = if (args.hasNext()) {
                val playerName = args.next()
                val player = Bukkit.getPlayerExact(playerName)

                if (player == null) {
                    sender.sendMessage("플레이어를 찾지 못했습니다.")
                    return true
                }

                player.esper
            } else {
                if (sender is Player) {
                    sender.esper
                } else {
                    return false
                }
            }

            esper?.applyPsychic(psychic)?.apply { enabled = true } ?: sender.sendMessage("알 수 없는 이유로 능력을 적용하지 못했습니다.")

        } ?: sender.sendMessage("$psychicName 능력을 찾지 못했습니다.")

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        label: String,
        componentLabel: String,
        args: ArgumentList
    ): List<String> {
        return when (args.remain()) {
            1 -> Psychics.storage.psychicSpecs.keys.tabComplete(args.last())
            2 -> Bukkit.getOnlinePlayers().tabComplete(args.last()) { it.name }
            else -> emptyList()
        }
    }
}

