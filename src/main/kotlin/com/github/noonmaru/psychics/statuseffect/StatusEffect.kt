/*
 *
 *  * Copyright (c) 2020 Noonmaru
 *  *
 *  * Licensed under the General Public License, Version 3.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://opensource.org/licenses/gpl-3.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  
 */

package com.github.noonmaru.psychics.statuseffect

import com.github.noonmaru.psychics.utils.currentTicks
import kotlin.math.max

open class StatusEffect {

    var durationTick: Int = 0
        set(value) {
            field = max(0, value)
            endTick = currentTicks + value
        }

    internal var endTick: Int = 0
        private set

    val remainTick
        get() = max(0, endTick - currentTicks)


}