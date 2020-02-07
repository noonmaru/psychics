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

import com.google.common.collect.ImmutableList
import org.bukkit.configuration.ConfigurationSection

class AbilityDescription(config: ConfigurationSection) {

    val name: String = config.findString("name")

    val main: String = config.findString("main")

    val depend: List<String> = config.findList("depend")

    val version: String = config.findString("version")

    val heroesVersion: String? = config.getString("heroes-version")

    val description: String? = config.getString("description")

    val authors: List<String> = config.findList("authors")

}

private fun ConfigurationSection.findString(key: String): String {
    return getString(key) ?: throw NullPointerException("$key is undefined in ability.yml")
}

private fun ConfigurationSection.findList(key: String): List<String> {
    return getStringList(key).takeIf { it.isNotEmpty() }?.run { ImmutableList.copyOf(this) } ?: emptyList()
}