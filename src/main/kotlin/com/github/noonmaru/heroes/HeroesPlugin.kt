package com.github.noonmaru.heroes

import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Nemo
 */
class HeroesPlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("Heroes ASSEMBLE!")
    }
}