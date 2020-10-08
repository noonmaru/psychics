package com.github.noonmaru.psychics.ability.boomerang

import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.ActiveAbility
import com.github.noonmaru.psychics.PsychicProjectile
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.psychics.util.Tick
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.fake.invisible
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.math.toRadians
import com.google.common.collect.ImmutableList
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

@Name("boomerang")
class AbilityBoomerangConcept : AbilityConcept() {

    @Config
    var boomerangItems = listOf(
        ItemStack(Material.RED_WOOL),
        ItemStack(Material.YELLOW_WOOL),
        ItemStack(Material.GREEN_WOOL),
        ItemStack(Material.BLUE_WOOL),
        ItemStack(Material.PURPLE_WOOL)
    )

    @Config
    var projectileLaunchSpeed = 7.0

    @Config
    var projectileTicks: Int = 200

    @Config
    var projectileReturningTicks = 8

    @Config
    var projectileReturningSpeed = 0.4

    @Config
    val knockback: Double = 0.75

    @Config
    val raySize: Double = 0.3

    init {
        displayName = "부메랑"
        cooldownTicks = 2
        wand = ItemStack(Material.GOLD_INGOT)
        damage = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 2.0))

        description = listOf(
            "지정한 방향으로 부메랑을 빠르게 발사합니다.",
            "발사된 부메랑은 부딪힌 적에게 피해를 입히고",
            "천천히 돌아옵니다.",
            "부메랑을 회수시 다시 사용 할 수 있습니다."
        )
    }
}

class AbilityBoomerang : ActiveAbility<AbilityBoomerangConcept>() {

    private lateinit var boomerangs: List<Boomerang>

    init {
        targeter = {
            esper.player.eyeLocation
        }
    }

    private fun getBackBoomerangLocation(): Location {
        return esper.player.location.apply {
            pitch = 0.0F
            y += 1.325

            subtract(direction)
        }
    }

    override fun onEnable() {
        val loc = getBackBoomerangLocation()
        boomerangs = ImmutableList.copyOf(
            concept.boomerangItems.map { Boomerang(loc, it) }
        )

        psychic.runTaskTimer({
            val center = getBackBoomerangLocation()
            val angle = Math.PI / boomerangs.count()
            val ticks = Tick.currentTicks / 10.0

            boomerangs.forEachIndexed { index, boomerang ->
                if (boomerang.vehicle == null) {
                    val z = sin(ticks + angle * index)
                    val vector = Vector(0.0, 0.0, z).rotateAroundX(angle * index)
                        .rotateAroundY(-(center.yaw.toDouble() + 90.0).toRadians())
                    boomerang.updateLocation(center.clone().add(vector))
                }
            }
        }, 0L, 1L)
    }

    override fun onDisable() {
        boomerangs.forEach {
            it.remove()
        }
        boomerangs = ImmutableList.of()
    }

    override fun tryCast(
        event: PlayerEvent,
        action: WandAction,
        castingTicks: Long,
        cost: Double,
        targeter: (() -> Any?)?
    ): TestResult {
        if (boomerangs.find { it.vehicle == null } == null)
            return TestResult.create("남아있는 부메랑이 없습니다.")

        return super.tryCast(event, action, castingTicks, cost, targeter)
    }

    override fun onCast(event: PlayerEvent, action: WandAction, target: Any?) {
        val availableBoomerang = boomerangs.find { it.vehicle == null } ?: return

        cooldownTicks = concept.cooldownTicks

        val eyeLocation = target as Location
        val projectile = BoomerangProjectile(availableBoomerang)
        availableBoomerang.vehicle = projectile

        psychic.launchProjectile(eyeLocation, projectile)
        projectile.velocity = eyeLocation.direction.multiply(concept.projectileLaunchSpeed)
        eyeLocation.world.playSound(eyeLocation, Sound.ENTITY_ARROW_SHOOT, 0.5F, 1.5F)
    }

    inner class Boomerang(location: Location, itemStack: ItemStack) {
        private val armorStand: FakeEntity =
            psychic.spawnFakeEntity(location.apply { y -= 1.62 }, ArmorStand::class.java).apply {
                updateMetadata<ArmorStand> {
                    rightArmPose = EulerAngle(0.0, Math.PI / 2.0, Math.PI)
                    invisible = true
                    isMarker = true
                }
                updateEquipment {
                    setItemInMainHand(itemStack)
                }
            }

        internal var vehicle: BoomerangProjectile? = null

        fun updateLocation(targetLocation: Location) {
            armorStand.moveTo(targetLocation.clone().apply { y -= 1.62 })
        }

        fun remove() {
            armorStand.remove()
        }
    }

    inner class BoomerangProjectile(
        private val boomerang: Boomerang
    ) : PsychicProjectile(concept.projectileTicks, 256.0) {
        override fun onPreUpdate() {
            val turningTicks = concept.projectileReturningTicks
            val remaining = turningTicks - ticks

            if (remaining > 0) {
                val speed = concept.projectileLaunchSpeed - concept.projectileLaunchSpeed * ticks / turningTicks
                velocity = velocity.normalize().multiply(speed)
            } else {
                val targetLoc = getReturnLocation()
                val location = location

                if (targetLoc.world != location.world) {
                    remove()
                } else {
                    val elapsed = abs(remaining)
                    var speed =
                        min(concept.projectileReturningSpeed, concept.projectileReturningSpeed * elapsed / turningTicks)
                    val vector = targetLoc.subtract(location).toVector()
                    val length = vector.normalizeAndLength()

                    //avoid infinity
                    if (length > 0) {
                        // 속도가 거리를 넘을경우 거리로 속도를 보정
                        if (speed > length) {
                            speed = length
                        }

                        velocity = vector.normalize().multiply(speed)
                    }
                }
            }
        }

        override fun onMove(movement: Movement) {
            val to = movement.to
            boomerang.updateLocation(to)

            if (ticks >= concept.projectileReturningTicks) {
                val targetLocation = getReturnLocation()

                if (targetLocation.world != to.world || targetLocation.distance(to) < 0.5) {
                    remove()
                }
            }
        }

        private fun getReturnLocation(): Location {
            return getBackBoomerangLocation().apply { y -= 0.45 }
        }

        private val hitEntities = hashSetOf<Entity>()

        override fun onTrail(trail: Trail) {
            val velocity = trail.velocity

            if (velocity != null && velocity.length() > 0.0 && ticks <= concept.projectileReturningTicks) {
                val from = trail.from
                val world = from.world
                val length = velocity.normalizeAndLength()

                world.rayTraceEntities(
                    from,
                    velocity,
                    length,
                    concept.raySize,
                    TargetFilter(esper.player).and { it !in hitEntities }
                )?.let { result ->
                    val entity = result.hitEntity

                    if (entity != null && entity is LivingEntity) {
                        hitEntities += entity
                        concept.damage?.let { damage ->
                            entity.psychicDamage(damage, result.hitPosition.toLocation(world), concept.knockback)
                        }
                    }
                }
            }
        }

        override fun onRemove() {
            boomerang.vehicle = null
            hitEntities.clear() // help gc

            val location = location
            location.world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.5F)
        }
    }
}