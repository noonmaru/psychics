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

import com.github.noonmaru.psychics.util.UpstreamReference
import com.google.common.base.Preconditions
import org.bukkit.Location
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import kotlin.math.max

open class Projectile(
    var maxTicks: Int,
    var maxRange: Double,
    var rayTracer: ((start: Location, direction: Vector, maxDistance: Double) -> RayTraceResult?)? = null
) {
    private lateinit var shooterRef: UpstreamReference<Psychic>

    val shooter: Psychic
        get() = shooterRef.get()

    private lateinit var _previousLocation: Location

    private lateinit var _currentLocation: Location

    private lateinit var _targetLocation: Location

    private lateinit var _velocity: Vector

    //interface
    val previousLocation
        get() = _previousLocation.clone()

    val currentLocation
        get() = _currentLocation.clone()

    var targetLocation
        get() = _targetLocation.clone()
        set(value) {
            _targetLocation.set(value)
        }

    var velocity
        get() = _velocity.clone()
        set(value) {
            _velocity.apply {
                x = value.x
                y = value.y
                z = value.z
            }
        }

    var ticks = 0
        private set

    var flyingDistance = 0.0
        private set

    val remainDistance
        get() = max(0.0, maxRange - flyingDistance)

    var rayTraceResult: RayTraceResult? = null
        private set

    var launched = false
        internal set

    var valid = true
        private set

    internal fun init(shooter: Psychic, spawnLocation: Location, velocity: Vector) {
        this.shooterRef = UpstreamReference(shooter)
        this._previousLocation = spawnLocation.clone()
        this._currentLocation = spawnLocation.clone()
        this._targetLocation = spawnLocation.clone()
        this._velocity = velocity.clone()
    }

    internal fun update() {
        runCatching { onPreUpdate() }

        val from = _currentLocation
        val to = _targetLocation
        val currentVector = to vector from
        val currentVectorLength = currentVector.length()
        //벡터 정규화
        currentVector.apply {
            x /= currentVectorLength
            y /= currentVectorLength
            z /= currentVectorLength
        }

        ticks++
        rayTracer?.runCatching {
            if (from.world == to.world) {
                invoke(from.clone(), currentVector.clone(), currentVectorLength)?.let { result ->
                    rayTraceResult = result
                    val v = result.hitPosition
                    _targetLocation.set(v.x, v.y, v.z)
                    remove()
                }
            }
        }

        _previousLocation.set(_currentLocation)
        _currentLocation.set(_targetLocation)
        flyingDistance += currentVectorLength

        when {
            ticks >= maxTicks -> {
                remove()
            }
            flyingDistance >= maxRange -> { //남은 비행거리 모두 소진
                remove()
            }
            else -> {
                var nextVelocity = _velocity
                var nextVelocityLength = nextVelocity.length()
                val remainDistance = remainDistance
                //남은 비행거리가 현재 속력보다 작을경우 최대사거리를 넘지 않기 위해 속력을 남은거리로 보정
                if (remainDistance < nextVelocityLength) {
                    nextVelocityLength = remainDistance
                    nextVelocity = nextVelocity.clone().apply {
                        x /= nextVelocityLength
                        y /= nextVelocityLength
                        z /= nextVelocityLength
                        multiply(remainDistance)
                    }
                }

                _targetLocation.add(nextVelocity)
            }
        }

        runCatching { onPostUpdate() }
    }

    open fun onPreUpdate() {}

    open fun onPostUpdate() {}

    fun remove() {
        if (valid) {
            valid = false
            onDestroy()
        }
    }

    open fun onDestroy() {}

    fun checkState() {
        Preconditions.checkState(this.valid, "Invalid ${javaClass.name} $this")
    }
}

fun playParticles(start: Location, end: Location, interval: Double, effector: (Location) -> Unit) {
    val direction = Vector(end.x - start.x, end.y - start.y, end.z - start.z)
    val length = direction.length()
    val count = max(1, (length / interval).toInt())

    playParticles(start, direction, interval, count, effector)
}

fun playParticles(start: Location, direction: Vector, interval: Double, count: Int, effector: (Location) -> Unit) {
    val effectLoc = start.clone()
    val effectVec = direction.clone().normalize().multiply(interval)

    for (i in 0 until count) {
        effectLoc.set(start)

        effectLoc.apply {
            effectVec.let { v ->
                x += v.x * i
                y += v.y * i
                z += v.z * i
            }
        }

        effector.invoke(effectLoc)
    }
}

private fun Location.set(other: Location) {
    world = other.world
    x = other.x
    y = other.y
    z = other.z
    yaw = other.yaw
    pitch = other.pitch
}

private infix fun Location.vector(from: Location): Vector {
    return Vector(x - from.x, y - from.y, z - from.z)
}

internal class ProjectileManager {

    private val projectiles = ArrayList<Projectile>()

    fun add(projectile: Projectile) {
        Preconditions.checkState(!projectile.launched, "Already launched Projectile $projectile")
        projectile.checkState()

        projectile.apply {
            launched = true
            projectiles.add(this)
        }
    }

    fun updateAll() {
        val iter = projectiles.iterator()

        while (iter.hasNext()) {
            iter.next().run {
                if (valid)
                    update()

                if (!valid)
                    iter.remove()
            }
        }
    }

    internal fun removeAll() {
        projectiles.run {
            forEach { it.remove() }
            clear()
        }
    }
}