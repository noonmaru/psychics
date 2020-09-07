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

package com.github.noonmaru.psychics.task

import kotlin.math.max

class TickTask internal constructor(
    private val scheduler: TickScheduler,
    val runnable: Runnable,
    delay: Long
) : Comparable<TickTask> {

    companion object {
        internal const val ERROR = 0L
        internal const val NO_REPEATING = -1L
        internal const val CANCEL = -2L
        internal const val DONE = -3L
    }

    internal var nextRunTick: Long = scheduler.currentTicks + max(0L, delay)

    internal var period: Long = 0L

    val isScheduled: Boolean
        get() = period.let { it != ERROR && it > CANCEL }

    val isCancelled
        get() = period == CANCEL

    val isDone
        get() = period == DONE

    internal fun execute() {
        runnable.runCatching { run() }
    }

    fun cancel() {
        if (!isScheduled) return

        period = CANCEL

        //256 tick 이상이면 큐에서 즉시 제거, 아닐경우 자연스럽게 제거
        val remainTicks = nextRunTick - scheduler.currentTicks

        if (remainTicks > 0xFF)
            scheduler.remove(this)
    }

    override fun compareTo(other: TickTask): Int {
        return this.nextRunTick.compareTo(other.nextRunTick)
    }
}

