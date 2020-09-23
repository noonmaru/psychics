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

package com.github.noonmaru.psychics.damage

import org.bukkit.enchantments.Enchantment

enum class DamageType(
    val protection: Enchantment,
    val i18Name: String
) {
    MELEE(Enchantment.PROTECTION_ENVIRONMENTAL, "근접 피해"), // 보호
    RANGED(Enchantment.PROTECTION_PROJECTILE, "원거리 피해"), // 원거리 보호
    FIRE(Enchantment.PROTECTION_FIRE, "화염 피해"), // 화염 보호
    BLAST(Enchantment.PROTECTION_EXPLOSIONS, "폭발 피해"), // 폭발 보호
}