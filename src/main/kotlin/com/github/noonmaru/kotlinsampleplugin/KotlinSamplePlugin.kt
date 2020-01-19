package com.github.noonmaru.kotlinsampleplugin

import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Nemo
 */
class KotlinSamplePlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("Hello Kotlin Plugin!")
    }
}