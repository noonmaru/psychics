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

import com.github.noonmaru.psychics.plugin.PsychicPlugin
import com.github.noonmaru.psychics.utils.FakeManager
import com.github.noonmaru.tap.event.EntityEventManager
import org.bukkit.entity.Player
import java.io.File
import java.util.logging.Logger
import com.github.noonmaru.tap.fake.FakeManager as TapFakeManager

object Psychics {

    lateinit var logger: Logger
        private set

    lateinit var entityEventBus: EntityEventManager
        private set

    lateinit var fakeManager: FakeManager
        private set

    lateinit var storage: PsychicStorage
        private set

    lateinit var esperManager: EsperManager
        private set

    internal fun initialize(plugin: PsychicPlugin) {
        logger = plugin.logger
        entityEventBus = EntityEventManager(plugin)
        fakeManager = FakeManager(TapFakeManager())

        val dir = plugin.dataFolder

        storage = PsychicStorage(File(dir, "abilities"), File(dir, "psychics"))
        esperManager = EsperManager(plugin, File(dir, "espers"))

        plugin.server.apply {
            scheduler.runTaskTimer(plugin, Scheduler(esperManager, fakeManager), 0L, 1L)
            pluginManager.registerEvents(EventListener(fakeManager), plugin)
        }
    }
}

val Player.esper: Esper?
    get() = Psychics.esperManager.getEsper(this)
