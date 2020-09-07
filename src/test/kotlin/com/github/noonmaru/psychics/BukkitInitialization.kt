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

import net.minecraft.server.v1_16_R2.DispenserRegistry
import net.minecraft.server.v1_16_R2.WorldServer
import org.apache.commons.lang.reflect.FieldUtils
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.craftbukkit.v1_16_R2.CraftServer
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemFactory
import org.bukkit.craftbukkit.v1_16_R2.util.Versioning
import org.mockito.Mockito
import org.spigotmc.SpigotWorldConfig
import java.util.logging.Logger

/**
 * @author Kristian
 */
class BukkitInitialization private constructor() {
    private var initialized = false

    init {
        println("Created new BukkitInitialization on " + Thread.currentThread().name)
    }

    private fun initialize0() {
        if (!initialized) {
            // Denote that we're done
            initialized = true
            try {
                LogManager.getLogger()
            } catch (ex: Throwable) {
                // Happens only on my Jenkins, but if it errors here it works when it matters
                ex.printStackTrace()
            }
            DispenserRegistry.init()

            // Mock the server object
            val mockedServer = Mockito.mock(Server::class.java)
            Mockito.`when`(mockedServer.logger).thenReturn(Logger.getLogger("Minecraft"))
            Mockito.`when`(mockedServer.name).thenReturn("Mock Server")
            Mockito.`when`(mockedServer.version)
                .thenReturn(CraftServer::class.java.getPackage().implementationVersion)
            Mockito.`when`(mockedServer.bukkitVersion).thenReturn(Versioning.getBukkitVersion())
            Mockito.`when`(mockedServer.itemFactory).thenReturn(CraftItemFactory.instance())
            Mockito.`when`(mockedServer.isPrimaryThread).thenReturn(true)
            val nmsWorld: WorldServer = Mockito.mock(WorldServer::class.java)
            val mockWorldConfig: SpigotWorldConfig = Mockito.mock(SpigotWorldConfig::class.java)
            try {
                FieldUtils.writeField(nmsWorld.javaClass.getField("spigotConfig"), nmsWorld, mockWorldConfig, true)
            } catch (ex: ReflectiveOperationException) {
                throw RuntimeException(ex)
            }
            val world: CraftWorld = Mockito.mock(CraftWorld::class.java)
            Mockito.`when`(world.handle).thenReturn(nmsWorld)
            val worlds: List<World> = listOf(world)
            Mockito.`when`(mockedServer.worlds).thenReturn(worlds)
            // Inject this fake server
            Bukkit.setServer(mockedServer)
        }
    }

    companion object {
        private val instance = BukkitInitialization()

        @JvmStatic
        @Synchronized
        fun initialize() {
            instance.initialize0()
        }
    }
}