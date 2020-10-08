package com.github.noonmaru.psychics.ability.straightshuriken

import com.github.noonmaru.psychics.*
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.effect.playFirework
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.math.copy
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.math.toRadians
import com.github.noonmaru.tap.trail.trail
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BoundingBox
import org.bukkit.util.EulerAngle
import kotlin.random.Random.Default.nextFloat

@Name("straight-shuriken")
class StraightShurikenConcept : AbilityConcept() {

    @Config
    var shurikenInitSpeed = 1.0

    @Config
    val shurikenAcceleration = 0.04

    @Config
    var shurikenTicks = 100

    @Config
    var shurikenExplosionRadius = 3.0

    @Config
    var shurikenSize = 1.0

    @Config
    var shurikenKnockback = 3.0

    init {
        displayName = "직선수리검"
        castingTicks = 30
        range = 127.0
        interruptible = true
        cost = 30.0
        wand = ItemStack(Material.HEART_OF_THE_SEA)
        damage = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 7.5))
        description = listOf(
            "바다의 심장을 우클릭 시 나선수리검을 발사합니다.",
            "발사된 나선수리검은 최대 \${common.range} 블록을 날아가며",
            "적 혹은 블록에 부딪힐 시 폭발을 일으켜 \${straight-shuriken.shuriken-explosion-radius} 블록",
            "내의 적에게 <damage>의 피해를 입힙니다."
        )
    }
}

class StraightShuriken : ActiveAbility<StraightShurikenConcept>() {
    companion object {
        internal val shurikenItem = ItemStack(Material.TRIDENT)
        internal const val headOffset = 1.1
    }

    private var shuriken: Shuriken? = null

    override fun tryCast(
        event: PlayerEvent,
        action: WandAction,
        castingTicks: Long,
        cost: Double,
        targeter: (() -> Any?)?
    ): TestResult {
        val ret = super.tryCast(event, action, castingTicks, cost, targeter)

        if (ret == TestResult.SUCCESS) {
            shuriken = Shuriken(esper.player.eyeLocation)
        }

        return ret
    }

    override fun onChannel(channel: Channel) {
        shuriken?.run {
            rotate()
            updateLocation(if (channel.remainingTicks > concept.castingTicks - 3) -60.0 else 0.0)
        }
    }

    override fun onInterrupt(channel: Channel) {
        shuriken?.remove()
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        shuriken?.let { shuriken ->
            val projectile = ShurikenProjectile(shuriken)
            psychic.launchProjectile(shuriken.location, projectile)
            projectile.velocity = shuriken.location.direction.multiply(concept.shurikenInitSpeed)
            this.shuriken = null
        }
    }

    inner class Shuriken(
        internal val location: Location
    ) {
        private val armorStands: MutableList<FakeEntity>

        private var rotateTicks = 0

        init {
            val armorStands = arrayListOf<FakeEntity>()

            val location = location.clone().apply { y += -60.0 - headOffset;yaw = 0.0F;pitch = 0.0F }

            repeat(32) {
                armorStands += psychic.spawnFakeEntity(location, ArmorStand::class.java).apply {
                    updateMetadata<ArmorStand> {
                        isMarker = true
                        isVisible = false
                        headPose = EulerAngle(90.0.toRadians(), 0.0, 90.0.toRadians())
                    }
                    updateEquipment {
                        helmet = shurikenItem
                    }
                }
            }

            armorStands.trimToSize()
            this.armorStands = armorStands
        }

        fun updateLocation(offsetY: Double = 0.0, newLoc: Location = location) {
            val x = newLoc.x
            val y = newLoc.y
            val z = newLoc.z

            location.apply {
                world = newLoc.world
                this.x = x
                this.y = y
                this.z = z
            }

            val count = armorStands.count()
            val loc = newLoc.clone()

            armorStands.forEachIndexed { index, armorStand ->
                armorStand.moveTo(loc.apply {
                    copy(newLoc)
                    this.y += offsetY - headOffset
                    yaw = (360.0 * index / count - rotateTicks * 12.5).toFloat()
                    pitch = 0.0F
                })
            }
        }

        fun rotate() {
            rotateTicks++
        }

        fun remove() {
            armorStands.forEach { it.remove() }
            armorStands.clear() //help gc
        }
    }

    inner class ShurikenProjectile(
        private val shuriken: Shuriken
    ) : PsychicProjectile(
        concept.shurikenTicks, concept.range
    ) {
        override fun onMove(movement: Movement) {
            shuriken.rotate()
            shuriken.updateLocation(0.0, movement.to)
        }

        override fun onTrail(trail: Trail) {
            trail.velocity?.let { velocity ->
                val from = trail.from
                val world = from.world
                val length = velocity.normalizeAndLength()
                val filter = TargetFilter(esper.player)

                world.rayTrace(
                    from,
                    velocity,
                    length,
                    FluidCollisionMode.NEVER,
                    true,
                    concept.shurikenSize,
                    filter
                )?.let { result ->
                    remove()

                    val hitPosition = result.hitPosition
                    val hitLocation = hitPosition.toLocation(world)

                    val firework =
                        FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.AQUA).build()
                    world.playFirework(hitLocation, firework)

                    concept.damage?.let { damage ->
                        val radius = concept.shurikenExplosionRadius
                        val box = BoundingBox.of(hitPosition, radius, radius, radius)

                        for (entity in world.getNearbyEntities(box, filter)) {
                            if (entity is LivingEntity) {
                                entity.psychicDamage(damage, hitLocation, concept.shurikenKnockback)
                            }
                        }
                    }
                }

                val to = trail.to

                trail(from, to, 0.25) { w, x, y, z ->
                    w.spawnParticle(
                        Particle.CRIT_MAGIC,
                        x, y, z,
                        5,
                        0.1, 0.1, 0.1,
                        0.25, null, true
                    )
                }

                to.world.playSound(to, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.MASTER, 0.25F, 1.8F + nextFloat() * 0.2F)
            }
        }

        override fun onPostUpdate() {
            velocity = velocity.multiply(1.0 + concept.shurikenAcceleration)
        }

        override fun onRemove() {
            shuriken.remove()
        }
    }
}

