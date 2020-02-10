/*
 * Copyright (c) 2020 Noonmaru
 *
 * Licensed under the General Public License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/gpl-2.0.php
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.noonmaru.heroes.loader

import com.github.noonmaru.heroes.psychic.Ability
import com.github.noonmaru.heroes.psychic.AbilityDescription
import com.github.noonmaru.heroes.psychic.AbilityStatement
import com.google.common.base.Preconditions
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile


class AbilityLoader internal constructor() {

    private val classes: MutableMap<String, Class<*>?> = ConcurrentHashMap()

    private val loaders: MutableMap<File, AbilityClassLoader> = ConcurrentHashMap()

    @Throws(Throwable::class)
    internal fun load(file: File): AbilityStatement {
        Preconditions.checkArgument(file !in loaders, "Already registered file ${file.name}")

        val desc = file.getAbilityDescription()
        AbilityClassLoader(javaClass.classLoader, this, file).use { classLoader ->
            //임시 클래스로더 생성 (메인 클래스를 못 찾을 경우 폐기)
            val abilityClass = Class.forName(desc.main).asSubclass(Ability::class.java) //메인 클래스 찾기
            abilityClass.newInstance() // 인스턴스 생성 테스트
            loaders[file] = classLoader // 글로벌 클래스 로더 등록
            return AbilityStatement(file, desc, classLoader, abilityClass)
        }
    }

    @Throws(ClassNotFoundException::class)
    fun findClass(name: String, skip: AbilityClassLoader): Class<*> {
        var found = classes[name]

        if (found != null) return found

        for (loader in loaders.values) {
            if (loader === skip) continue

            try {
                found = loader.findLocalClass(name)
                classes[name] = found

                return found
            } catch (ignore: ClassNotFoundException) {
            }
        }

        throw ClassNotFoundException(name)
    }
}

private fun File.getAbilityDescription(): AbilityDescription {
    JarFile(this).use { jar ->
        jar.getJarEntry("ability.yml")?.let { entry ->
            jar.getInputStream(entry).bufferedReader(UTF_8).use { reader ->
                val config = YamlConfiguration.loadConfiguration(reader)
                return AbilityDescription(config)
            }
        }
    }

    throw IllegalArgumentException("Failed to open JarFile '${this.name}'")
}