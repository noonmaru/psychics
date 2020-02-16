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

package com.github.noonmaru.psychics.task

import com.github.noonmaru.psychics.utils.currentTicks
import java.util.*
import kotlin.math.max

class PsychicTask internal constructor(private val scheduler: PsychicScheduler, val runnable: Runnable, delay: Long) {

    companion object {
        internal const val ERROR = 0L
        internal const val NO_REPEATING = -1L
        internal const val CANCEL = -2L
        internal const val DONE = -3L
    }

    internal var nextRun: Long = currentTicks + max(0L, delay)

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
        val remainTicks = nextRun - currentTicks

        if (remainTicks > 0xFF)
            scheduler.remove(this)
    }
}

class PsychicScheduler : Runnable {

    private val queue = PriorityQueue<PsychicTask>()

    fun runTask(runnable: Runnable, delay: Long): PsychicTask {
        PsychicTask(this, runnable, delay).apply {
            this.period = PsychicTask.NO_REPEATING
            queue.offer(this)
            return this
        }
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): PsychicTask {
        PsychicTask(this, runnable, delay).apply {
            this.period = max(1L, period)
            queue.offer(this)
            return this
        }
    }

    override fun run() {
        val current = currentTicks

        while (queue.isNotEmpty()) {
            val task = queue.peek()

            if (task.nextRun < current)
                break

            queue.remove()

            if (task.isScheduled) {

                task.run {
                    execute()
                    if (period > 0) {
                        nextRun = current + period
                        queue.offer(task)
                    } else {
                        period == PsychicTask.DONE
                    }
                }
            }
        }
    }

    internal fun cancelAll() {
        val queue = this.queue
        queue.forEach { it.period = PsychicTask.CANCEL }
        queue.clear()
    }

    fun remove(task: PsychicTask) {
        queue.remove(task)
    }
}