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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.powermock.reflect.Whitebox
import java.util.*

class TickSchedulerTest {

    private var countA = 0
    private var countB = 0

    @Test
    fun test() {
        val scheduler = TickScheduler(1)
        val queue = Whitebox.getInternalState<PriorityQueue<TickTask>>(scheduler, "queue")

        val taskA = scheduler.runTask({
            countA++
        }, 0L)

        val taskB = scheduler.runTaskTimer({
            countB++
        }, 0L, 1L)

        assertEquals(2, queue.count())

        scheduler.run()
        assertEquals(1, countA)
        assertEquals(1, countB)
        assertEquals(1, queue.count())
        assertTrue(taskA.isDone)
        assertTrue(taskB.isScheduled)

        scheduler.run()
        assertEquals(1, countA)
        assertEquals(2, countB)
        assertEquals(1, queue.count())
        assertTrue(taskB.isScheduled)

        //cancel and run
        scheduler.run()
        taskB.cancel() //lazy remove
        assertEquals(3, countB)
        assertEquals(1, queue.count())
        assertTrue(taskB.isCancelled)

        scheduler.run()
        assertEquals(3, countB)
        assertEquals(0, queue.count())
    }
}