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

package com.github.noonmaru.psychics.cmd

import com.github.noonmaru.psychics.AbilitySpec
import com.github.noonmaru.psychics.PsychicSpec
import com.github.noonmaru.psychics.Psychics
import com.github.noonmaru.psychics.esper
import com.github.noonmaru.tap.command.ArgumentList
import com.github.noonmaru.tap.command.CommandComponent
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

class CommandInfo : CommandComponent {
    override fun test(sender: CommandSender): (() -> String)? {
        return if (sender is Player) null else {
            { "콘솔에서 사용 할 수 없는 명령입니다." }
        }
    }

    override fun onCommand(sender: CommandSender, label: String, componentLabel: String, args: ArgumentList): Boolean {
        sender as Player

        val psychic = if (args.hasNext()) {
            val psychicName = args.next()
            Psychics.storage.psychicSpecs[psychicName].let {
                if (it == null) {
                    sender.sendMessage("$psychicName 능력을 찾지 못했습니다.")
                    return true
                } else {
                    it
                }
            }
        } else {
            val psy = sender.esper?.psychic ?: return false
            psy.spec
        }

        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = (book.itemMeta as BookMeta).apply {
            title = "${ChatColor.GOLD}능력: ${psychic.displayName}"
            generation = BookMeta.Generation.ORIGINAL
            author = "${ChatColor.BOLD}PSYCHICS"
            addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }

        val list = ArrayList<BaseComponent>()

        psychic.let { spec ->
            list += TextComponent(spec.displayName).apply {
                color = ChatColor.GOLD
                isUnderlined = true
                isBold = true
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, spec.info())
            }
        }

        list += TextComponent("\n\n\nABILITIES:").apply {
            isBold = true
        }

        psychic.abilities.forEach { spec ->
            list += TextComponent("\n\n - ${spec.displayName}").apply {
                color = ChatColor.RED
                isBold = true
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, spec.info())
            }
        }

        meta.spigot().addPage(list.toTypedArray())
        book.itemMeta = meta
        sender.inventory.addItem(book)

        return true
    }
}

fun AbilitySpec.info(): Array<BaseComponent> {
    val builder = ComponentBuilder("")

    builder.append("${type.name}\n").apply {
        color(ChatColor.GOLD)
        bold(true)
    }
    cooldownTicks.let {
        if (it > 0) {
            builder.appendInfo("재사용 대기시간", it / 20.0) {
                color(ChatColor.BLUE)
            }
        }
    }
    channelDuration.let {
        if (it > 0) {
            builder.appendInfo("시전 시간", it / 20.0) {
                color(ChatColor.GREEN)
            }
        }
    }
    cost.let {
        if (it > 0) {
            builder.appendInfo("마나 소모", it) {
                color(ChatColor.AQUA)
            }
        }
    }
    range.let {
        if (it > 0) {
            builder.appendInfo("사거리", it) {
                color(ChatColor.RED)
            }
        }
    }

    description.forEach {
        builder.append("\n$it").reset()
    }

    return builder.create()
}

fun PsychicSpec.info(): Array<BaseComponent> {
    val builder = ComponentBuilder("")
    var count = 0

    mana.let {
        if (it > 0) {
            builder.appendInfo("마나", it) {
                color(ChatColor.AQUA)
            }
            count++
        }
    }
    manaRegenPerSec.let {
        if (it > 0) {
            builder.appendInfo("마나재생", it) {
                color(ChatColor.DARK_AQUA)
            }
            count++
        }
    }

    description.forEach {
        builder.append("\n$it").reset()
    }

    return builder.create()
}

private fun ComponentBuilder.appendInfo(name: String, value: Any, applier: (ComponentBuilder.() -> Unit)? = null) {
    append(name).let {
        underlined(true)
        bold(true)
        applier?.invoke(it)
    }
    append("  ").reset()
    append("$value\n").apply {
        bold(true)
    }
}