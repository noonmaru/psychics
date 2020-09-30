package com.github.noonmaru.psychics.ability.magicarrow

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.AbilityType
import com.github.noonmaru.psychics.TestResult
import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.Damage
import com.github.noonmaru.psychics.damage.DamageType
import com.github.noonmaru.psychics.damage.psychicDamage
import com.github.noonmaru.psychics.item.isPsychicbound
import com.github.noonmaru.psychics.task.TickTask
import com.github.noonmaru.psychics.util.TargetFilter
import com.github.noonmaru.tap.config.Config
import com.github.noonmaru.tap.config.Name
import com.github.noonmaru.tap.config.RangeDouble
import com.github.noonmaru.tap.config.RangeLong
import com.github.noonmaru.tap.effect.playFirework
import com.github.noonmaru.tap.trail.trail
import net.md_5.bungee.api.ChatColor
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.random.Random.Default.nextFloat

@Name("magic-archer")
class MagicArrowConcept : AbilityConcept() {
    @Config
    @RangeDouble(min = 0.0)
    var arrowSize: Double = 0.5

    @Config
    @RangeLong(min = 0)
    var arrowDelayTicks: Long = 8L

    init {
        type = AbilityType.ACTIVE
        displayName = "마법화살"
        levelRequirement = 10
        range = 32.0
        cost = 40.0
        damage = Damage(DamageType.RANGED, EsperStatistic.of(EsperAttribute.ATTACK_DAMAGE to 3.0))
        supplyItems = listOf(
            ItemStack(Material.BOW).apply {
                val meta = itemMeta
                meta.setDisplayName("${ChatColor.LIGHT_PURPLE}${ChatColor.BOLD}마법 활")
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, false)
                meta.isUnbreakable = true
                meta.isPsychicbound = true
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
                itemMeta = meta
            },
            ItemStack(Material.ARROW).apply {
                isPsychicbound = true
            }
        )
        description = listOf(
            "활을 사용시 일반화살 대신 마법화살을 발사합니다.",
            "발사된 마법화살은 \${magic-archer.arrow-delay-ticks / 20.0}초 뒤 직선으로 발사되며",
            "맞은 적에게 피해를 입힙니다.",
            "마법화살의 사거리는 활시위 당김에 비례합니다."
        )
    }
}

class MagicArrow : Ability<MagicArrowConcept>(), Listener {
    override fun onEnable() {
        psychic.apply {
            registerEvents(this@MagicArrow)
        }
    }

    @EventHandler
    fun onPlayerShootEvent(event: EntityShootBowEvent) {
        val projectile = event.projectile
        projectile.remove()

        event.bow?.let { bow ->
            val testResult = test()

            if (testResult != TestResult.SUCCESS) {
                esper.player.sendActionBar(testResult.getMessage(this))
                return
            }

            cooldownTicks = concept.cooldownTicks
            psychic.consumeMana(concept.cost)

            val from = esper.player.eyeLocation
            val direction = from.direction
            from.subtract(direction)

            val magicTask = MagicTask(from, from.direction, bow, concept.range * event.force)
            val tickTask = psychic.runTaskTimer(magicTask, 0L, 1L)

            magicTask.task = tickTask
        }
    }

    inner class MagicTask(
        private val from: Location,
        private val direction: Vector,
        private val bow: ItemStack,
        private val range: Double
    ) : Runnable {
        var task: TickTask? = null
        private var ticks = 0

        override fun run() {
            val world = from.world

            if (++ticks < concept.arrowDelayTicks) {
                val x = from.x
                val y = from.y
                val z = from.z

                world.spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    x, y, z,
                    10,
                    0.00, 0.00, 0.00,
                    0.005,
                    null,
                    true
                )
            } else {
                task?.run {
                    cancel()
                    task = null
                }

                var to: Location = from.clone().add(direction.clone().multiply(range))

                processRayTrace()?.let { to = it }
                spawnParticles(to)
                playSound()
            }
        }

        private fun playSound() {
            from.world.playSound(
                from,
                Sound.ENTITY_ARROW_SHOOT,
                1.0F,
                0.8F + nextFloat() * 0.4F
            )
        }

        private fun processRayTrace(): Location? {
            from.world.rayTrace(
                from,
                direction,
                range,
                FluidCollisionMode.NEVER,
                true,
                concept.arrowSize,
                TargetFilter(esper.player)
            )?.let { rayTraceResult ->
                val to = rayTraceResult.hitPosition.toLocation(from.world)

                val firework = FireworkEffect.builder().with(FireworkEffect.Type.BURST)
                    .withColor(if (rayTraceResult.hitEntity != null) Color.RED else Color.GRAY).build()
                from.world.playFirework(to, firework)

                rayTraceResult.hitEntity?.let { target ->
                    if (target is LivingEntity) {
                        val damage = requireNotNull(concept.damage)
                        var damageAmount = esper.getStatistic(damage.stats)
                        val power = bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE)
                        val knockback = bow.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK)

                        if (power > 0) {
                            damageAmount += damageAmount * (1 + power) / 4.0
                        }

                        target.psychicDamage(
                            this@MagicArrow,
                            damage.type,
                            damageAmount,
                            esper.player,
                            from,
                            1.0 + knockback.toDouble()
                        )
                    }
                }

                return to
            }

            return null
        }

        private fun spawnParticles(to: Location) {
            trail(from, to, 0.25) { w, x, y, z ->
                w.spawnParticle(
                    Particle.CRIT,
                    x, y, z,
                    2,
                    0.05, 0.05, 0.05,
                    0.0,
                    null,
                    true
                )
            }
        }
    }
}