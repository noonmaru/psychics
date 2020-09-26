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

package com.github.noonmaru.psychics

import org.bukkit.configuration.ConfigurationSection

class AbilityDescription(config: ConfigurationSection) {
    val group: String = requireNotNull(config.getString("group")) { "gruop is not defined" }

    val name: String = requireNotNull(config.getString("name")) { "name is not defined" }

    val artifactId = "$group.$name"

    val main: String = requireNotNull(config.getString("main")) { "main is not defined" }

    val version: String = requireNotNull(config.getString("version")) { "version is not defined" }

    val author: String? = config.getString("author")
}