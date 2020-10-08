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

import com.github.noonmaru.psychics.attribute.EsperAttribute
import com.github.noonmaru.psychics.attribute.EsperStatistic
import com.github.noonmaru.psychics.damage.DamageSupport
import com.github.noonmaru.psychics.item.removeAllPsychicbounds
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class Esper(
    val manager: PsychicManager,
    player: Player
) {
    val player: Player
        get() = requireNotNull(playerRef.get()) { "Cannot get reference as it has already been Garbage Collected" }

    private val playerRef = WeakReference(player)

    private val attributeUniqueId: UUID

    var psychic: Psychic? = null
        private set

    val isOnline
        get() = playerRef.get() != null

    private val dataFile
        get() = File(manager.esperFolder, "${player.uniqueId}.yml")

    init {
        val uniqueId = player.uniqueId

        attributeUniqueId = UUID(uniqueId.leastSignificantBits.inv(), uniqueId.mostSignificantBits.inv())
    }

    /**
     * 능력치를 가져옵니다.
     */
    fun getAttribute(attr: EsperAttribute): Double {
        return when (attr) {
            EsperAttribute.ATTACK_DAMAGE -> DamageSupport.calculateAttackDamage(player.level)
            EsperAttribute.LEVEL -> player.level.toDouble()
            EsperAttribute.DEFENSE -> player.getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0
            EsperAttribute.HEALTH -> player.health
            EsperAttribute.MANA -> psychic?.mana ?: 0.0
        }
    }

    fun getStatistic(stats: EsperStatistic): Double {
        var ret = 0.0

        for ((attr, ratio) in stats.stats) {
            val value = getAttribute(attr)

            ret += value * ratio
        }

        return ret
    }

    private fun setPsychic(concept: PsychicConcept): Psychic {
        val psychic = concept.createInstance()
        this.psychic = psychic
        psychic.attach(this@Esper)
        updateAttribute()
        return psychic
    }

    fun attachPsychic(concept: PsychicConcept): Psychic {
        detachPsychic()

        return setPsychic(concept)
    }

    internal fun removePsychic() {
        psychic?.let { psychic ->
            this.psychic = null
            psychic.destroy()
            updateAttribute()
        }
    }

    fun detachPsychic() {
        removePsychic()
        player.inventory.removeAllPsychicbounds()
    }

    private fun updateAttribute() {
        val player = player
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.let { maxHealth ->
            val healthBonus = psychic?.concept?.healthBonus ?: 0.0
            val modifier =
                AttributeModifier(attributeUniqueId, "Psychics", healthBonus, AttributeModifier.Operation.ADD_NUMBER)

            maxHealth.removeModifier(modifier)
            maxHealth.addModifier(modifier)
        }
    }

    companion object {
        private const val PSYCHIC = "psychic"
    }

    internal fun load() {
        val file = dataFile

        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)

        config.getConfigurationSection(PSYCHIC)?.let { psychicConfig ->
            val psychicName = psychicConfig.getString(Psychic.NAME)

            if (psychicName != null) {
                val psychicConcept = manager.getPsychicConcept(psychicName)

                if (psychicConcept == null) {
                    manager.plugin.logger.warning("Failed to attach psychic $psychicName for ${player.name}")
                    return
                }

                val psychic = setPsychic(psychicConcept)
                psychic.load(psychicConfig)
            }
        }
    }

    fun save() {
        val config = YamlConfiguration()

        psychic?.save(config.createSection(PSYCHIC))

        config.save(dataFile)
    }

    internal fun clear() {
        psychic?.let { psychic ->
            psychic.destroy()
            this.psychic = null
        }
        playerRef.clear()
    }
}