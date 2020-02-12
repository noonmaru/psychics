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
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandApply : CommandComponent {

    override val argsCount: Int
        get() = 1

    override fun onCommand(sender: CommandSender, label: String, componentLabel: String, args: ArgumentList): Boolean {
        val esper = if (args.remain() == 1) {
            if (sender is Player) {
                sender.esper
            } else {
                sender.sendMessage("콘솔에서 사용 할 수 없는 명령입니다.")
                return true
            }
        } else {
            val name = args.next()
            Bukkit.getPlayerExact(name)?.esper
        }

        if (esper != null) {
            val psychicName = args.next()
            val psychicSpec = Psychics.storage.psychicSpecs[psychicName]

            if (psychicSpec != null) {
                esper.applyPsychic(psychicSpec)
                sender.sendMessage("${esper.player.name}에게 ${psychicSpec.name}(을)를 적용했습니다.")
            } else {
                sender.sendMessage("능력을 찾지 못했습니다. $psychicName")
            }
        } else {
            sender.sendMessage("Non esper player")
        }

        return true
    }
}

