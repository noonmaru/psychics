package com.github.noonmaru.psychics.ability.flyingsword

import com.github.noonmaru.psychics.*
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.damage.psychicDamage
import com.github.noonmaru.psychics.tooltip.TooltipBuilder
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.psychics.util.Tick
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.fake.FakeEntity
import com.github.noonmaru.tap.fake.Movement
import com.github.noonmaru.tap.fake.Trail
import com.github.noonmaru.tap.math.copy
import com.github.noonmaru.tap.math.normalizeAndLength
import com.github.noonmaru.tap.math.toRadians
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.EulerAngle
import java.util.*
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Name("flying-sword")
class FlyingSwordConcept : AbilityConcept() {

    @Config
    var projectileReadyTicks = 8

    @Config
    var summonTicks = 20

    @Config
    var projectileTicks = 40

    @Config
    var projectileSpeed = 4.0

    @Config
    var maxSummonCount = 5

    @Config
    var raySize = 1.0

    @Config
    var wooden = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 1.0))

    @Config
    var stone = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 1.5))

    @Config
    var iron = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 2.0))

    @Config
    var golden = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 2.5))

    @Config
    var diamond = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 3.0))

    @Config
    var netherite = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 3.5))

    @Config
    var bonusDamageBySharpness = 0.1

    init {
        displayName = "비도"
        type = AbilityType.ACTIVE
        range = 32.0
        cooldownTicks = 10
        cost = 10.0

        description = listOf(
            "검을 우클릭하여 비검을 소환합니다.",
            "최대 \${flying-sword.max-summon-count}개 소환 가능하며",
            "좌클릭으로 발사 할 수 있습니다.",
            "적에게 적중시 다음 피해를 입힙니다.",
            "나무: <wooden><wooden-damage>",
            "돌: <stone><stone-damage>",
            "철: <iron><iron-damage>",
            "금: <golden><golden-damage>",
            "다이아몬드: <diamond><diamond-damage>",
            "네더라이트: <netherite><netherite-damage>",
            "날카로움 인챈트당 \${flying-sword.bonus-damage-by-sharpness * 100}%의 추가 피해를 입힙니다."
        )
    }

    override fun onRenderTooltip(tooltip: TooltipBuilder, stats: (EsperStatistic) -> Double) {
        tooltip.addTemplates(
            "wooden" to stats(wooden.stats),
            "wooden-damage" to wooden,
            "stone" to stats(stone.stats),
            "stone-damage" to stone,
            "iron" to stats(iron.stats),
            "iron-damage" to iron,
            "golden" to stats(golden.stats),
            "golden-damage" to golden,
            "diamond" to stats(diamond.stats),
            "diamond-damage" to diamond,
            "netherite" to stats(netherite.stats),
            "netherite-damage" to netherite
        )
    }

    internal fun getDamageByType(type: Material): Damage? {
        return when (type) {
            Material.WOODEN_SWORD -> wooden
            Material.STONE_SWORD -> stone
            Material.IRON_SWORD -> iron
            Material.GOLDEN_SWORD -> golden
            Material.DIAMOND_SWORD -> diamond
            Material.NETHERITE_SWORD -> netherite
            else -> null
        }
    }
}

class FlyingSword : Ability<FlyingSwordConcept>(), Listener {
    // 머리 위 소환된 검 목록
    private lateinit var swords: LinkedList<Sword>

    override fun onAttach() {
        swords = LinkedList()
    }

    override fun onEnable() {
        psychic.run {
            registerEvents(this@FlyingSword)
            runTaskTimer({
                val center = getSwordLocation()
                val swords = swords
                val count = swords.count()
                val radius = 2.0

                swords.forEachIndexed { index, sword ->
                    val updateLoc = center.clone().apply {
                        val rotateTicks = 400
                        val yawOffset = (Tick.currentTicks % rotateTicks) / rotateTicks.toFloat() * 360.0F
                        val yaw = yawOffset + (360.0 * index / count).toFloat()
                        x += -sin(yaw.toDouble().toRadians()) * radius
                        z += cos(yaw.toDouble().toRadians()) * radius

                        this.yaw = yaw - 15.0F
                    }
                    sword.update(updateLoc)
                }
            }, 0L, 1L)
        }
    }

    fun getSwordLocation(): Location {
        return esper.player.eyeLocation.apply {
            y += 1.0
            yaw = 0.0F
            pitch = 0.0F
        }
    }

    override fun onDisable() {
        swords.run {
            forEach { it.remove() }
            clear()
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action == Action.PHYSICAL) return

        event.item?.let { item ->
            val type = item.type

            concept.getDamageByType(type)?.let { damage ->
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (test() == TestResult.SUCCESS && swords.count() < concept.maxSummonCount) {
                        swords.add(Sword(item.clone(), damage, getSwordLocation()))
                        cooldownTicks = concept.cooldownTicks
                        psychic.consumeMana(concept.cost)

                        val cooldown = concept.cooldownTicks.toInt()

                        esper.player.run {
                            setCooldown(Material.WOODEN_SWORD, cooldown)
                            setCooldown(Material.STONE_SWORD, cooldown)
                            setCooldown(Material.IRON_SWORD, cooldown)
                            setCooldown(Material.GOLDEN_SWORD, cooldown)
                            setCooldown(Material.DIAMOND_SWORD, cooldown)
                            setCooldown(Material.NETHERITE_SWORD, cooldown)
                        }
                    }
                } else {
                    val sword = swords.firstOrNull()
                    if (sword == null || sword.ticks < concept.summonTicks) return

                    swords.remove()
                    val eyeLocation = esper.player.eyeLocation
                    val direction = eyeLocation.direction
                    val projectile = SwordProjectile(sword).apply {
                        velocity = direction.multiply(concept.projectileSpeed)
                    }
                    sword.apply {
                        isLaunched = true
                        sword.updateHeadPose(eyeLocation.pitch)
                    }

                    psychic.launchProjectile(eyeLocation, projectile)
                }
            }
        }
    }

    //검 객체
    inner class Sword(
        internal val sword: ItemStack,
        internal val damage: Damage,
        private val loc: Location
    ) {
        internal var ticks: Int = 0
        private val entity: FakeEntity
        var isLaunched = false

        init {
            entity = psychic.spawnFakeEntity(loc.clone().apply { y += yOffset() }, ArmorStand::class.java).apply {
                updateMetadata<ArmorStand> {
                    isMarker = true
                    isVisible = false
                    headPose = EulerAngle(0.0, 0.0, (-45.0).toRadians())
                }
                updateEquipment {
                    helmet = sword
                }
            }
        }

        private fun yOffset(): Double {
            val summonTicks = concept.summonTicks

            if (ticks > summonTicks) return 0.0

            val remainTicks = ticks - summonTicks
            val pow = (remainTicks.toDouble() / summonTicks.toDouble()).pow(2.0)

            return pow * 32.0
        }

        fun update(updateLoc: Location) {
            ticks++
            var yOffset = 0.0
            val yawOffset = 90.0F
            if (!isLaunched) {
                //발사 전에 높이 조절
                yOffset += yOffset()
            }

            this.loc.copy(updateLoc)
            entity.moveTo(updateLoc.clone().apply {
                yaw += yawOffset
                y += yOffset - 1.62
            })
        }

        fun updateHeadPose(pitch: Float) {
            entity.updateMetadata<ArmorStand> {
                headPose = EulerAngle(0.0, 0.0, (45.0 + pitch).toRadians())
            }
        }

        fun remove() {
            entity.remove()
        }
    }

    //검 발사체
    inner class SwordProjectile(
        private val sword: Sword
    ) : PsychicProjectile(concept.projectileTicks, concept.range) {
        override fun onMove(movement: Movement) {
            if (ticks < concept.projectileReadyTicks)
                movement.to = movement.from.clone().add(velocity.multiply(0.01))

            sword.update(movement.to)
        }

        override fun onTrail(trail: Trail) {
            trail.velocity?.let { velocity ->
                val from = trail.from
                val direction = velocity.clone()
                val length = direction.normalizeAndLength()

                from.world.rayTrace(
                    from, direction, length, FluidCollisionMode.NEVER,
                    true, concept.raySize,
                    TargetFilter(esper.player)
                )?.let { result ->
                    remove()

                    result.hitEntity?.let { target ->
                        if (target is LivingEntity) {
                            val damage = sword.damage
                            var damageAmount = esper.getStatistic(damage.stats)

                            val item = sword.sword
                            val sharpness = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL)
                            val knockback = item.getEnchantmentLevel(Enchantment.KNOCKBACK)

                            damageAmount *= 1.0 + sharpness * concept.bonusDamageBySharpness

                            target.psychicDamage(
                                this@FlyingSword,
                                damage.type,
                                damageAmount,
                                esper.player,
                                result.hitPosition.toLocation(from.world),
                                1.0 + knockback.toDouble() * 2.0
                            )
                        }
                    }
                }
            }
        }

        override fun onRemove() {
            sword.remove()
        }
    }
}

