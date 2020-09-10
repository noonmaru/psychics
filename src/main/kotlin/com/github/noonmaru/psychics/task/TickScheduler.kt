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

import java.util.*
import kotlin.math.max

class TickScheduler : Runnable {
    private val queue = PriorityQueue<TickTask>()

    private val initNanoTime = System.nanoTime()

    var currentTicks = 0L
        private set

    fun runTask(runnable: Runnable, delay: Long): TickTask {
        TickTask(this, runnable, delay).apply {
            this.period = TickTask.NO_REPEATING
            queue.offer(this)
            return this
        }
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): TickTask {
        TickTask(this, runnable, delay).apply {
            this.period = max(1L, period)
            queue.offer(this)
            return this
        }
    }

    private fun nextTick(): Long {
        val elapsedNanoTime = System.nanoTime() - initNanoTime
        val currentTicks = elapsedNanoTime / (50L * 1000L * 1000L)
        this.currentTicks = currentTicks

        return currentTicks
    }

    override fun run() {
        val queue = queue
        val currentTick = nextTick()

        while (queue.isNotEmpty()) {
            val task = queue.peek()

            if (task.nextRunTick > currentTick)
                break

            queue.remove()

            if (task.isScheduled) {
                task.run {
                    execute()
                    if (period > 0L) {
                        nextRunTick = currentTick + period
                        queue.offer(task)
                    } else {
                        period = TickTask.DONE
                    }
                }
            }
        }
    }

    internal fun cancelAll() {
        val queue = this.queue
        queue.forEach { it.period = TickTask.CANCEL }
        queue.clear()
    }

    fun remove(task: TickTask) {
        queue.remove(task)
    }
}