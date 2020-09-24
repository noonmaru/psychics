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

package com.github.noonmaru.psychics.tooltip

import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.format.decimalFormat
import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.ItemStack

class TooltipBuilder {
    var title: String? = null
    private val stats = ArrayList<Triple<ChatColor, String, String>>()
    private val description = ArrayList<String>()
    private val footers = ArrayList<Pair<ChatColor, String>>()
    private val templates = HashMap<String, String>()

    fun addStats(color: ChatColor, name: String, value: String?) {
        if (value == null) return

        stats += Triple(color, name, value)
    }

    fun addStats(color: ChatColor, name: String, value: Number?, suffix: String? = null) {
        if (value == null || value.toDouble() == 0.0) return

        if (suffix == null)
            addStats(color, name, value.decimalFormat())
        else
            addStats(color, name, "${value.decimalFormat()}$suffix")
    }

    fun addStats(color: ChatColor, name: String, value: Any?) {
        if (value == null) return

        addStats(color, name, value.toString())
    }

    fun addDescription(c: Collection<String>) {
        description.addAll(c)
    }

    fun addFooter(footer: Pair<ChatColor, String>) {
        this.footers += footer
    }

    fun addTemplates(vararg templates: Pair<String, Any>) {
        for (template in templates) {
            val name = template.first
            require(name !in this.templates) { "Template name $name already in use" }
        }

        templates.forEach {
            val value = it.second

            val template = if (value is Number) value.decimalFormat() else value.toString()

            this.templates[it.first] = template
        }
    }

    fun toLore(includeTitle: Boolean = true): List<String> {
        val stats = stats
        val description = description
        val footers = footers
        val lore = ArrayList<String>()

        if (includeTitle) title?.let { lore += "$it${ChatColor.WHITE}" }

        if (stats.isNotEmpty()) {
            for ((color, name, value) in stats) {
                lore += "${color}${ChatColor.BOLD}$name${ChatColor.WHITE}  $value${ChatColor.WHITE}"
            }
        }

        if (description.isNotEmpty()) {
            lore += ""
            lore.addAll(description.map { "${ChatColor.GRAY}$it" })
        }

        if (footers.isNotEmpty()) {
            lore += ""
            lore.addAll(footers.map { (color, text) -> "$color$text" })
        }

        return lore.map { it.renderTemplates(templates::get) }
    }

    fun toItemStack(origin: ItemStack): ItemStack {
        return origin.clone().apply {
            itemMeta = itemMeta.apply {
                setDisplayName(title.toString())
                lore = toLore(false)
            }
        }
    }

    override fun toString(): String {
        return toLore().joinToString("\n")
    }
}

fun TooltipBuilder.addStats(color: ChatColor, name: String, template: String, statistics: EsperStatistic?) {
    if (statistics == null) return

    addStats(color, name, "$template$statistics")
}

fun TooltipBuilder.addStats(template: String, damage: Damage?) {
    if (damage == null) return

    addStats(ChatColor.DARK_PURPLE, damage.type.i18Name, "<$template>${damage.stats}")
}

fun String.renderTemplates(renderer: (name: String) -> String?): String {
    val builder = StringBuilder(this)
    builder.renderTemplates(0, renderer)

    return builder.toString()
}

private fun StringBuilder.renderTemplates(startIndex: Int, renderer: (name: String) -> String?): Int {
    var left = startIndex

    while (left < count()) {
        val c = this[left]

        if (c == '<') {
            val right = renderTemplates(left + 1, renderer)

            if (left < right) {
                val name = substring(left + 1, right)
                val value = renderer(name) ?: "NULL"
                require(!value.contains(Regex("[<>]"))) { "Rendered string cannot contains < or >" }

                replace(left, right + 1, value)
            }
        } else if (c == '>')
            return left

        left++
    }

    return -1
}