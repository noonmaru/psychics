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

package com.github.noonmaru.psychics.loader

import com.github.noonmaru.psychics.Ability
import com.github.noonmaru.psychics.AbilityConcept
import com.github.noonmaru.psychics.AbilityContainer
import com.github.noonmaru.psychics.AbilityDescription
import java.io.File
import java.util.concurrent.ConcurrentHashMap


class AbilityLoader internal constructor() {
    private val classes: MutableMap<String, Class<*>?> = ConcurrentHashMap()

    private val classLoaders: MutableMap<File, AbilityClassLoader> = ConcurrentHashMap()

    @Throws(Throwable::class)
    internal fun load(file: File, description: AbilityDescription): AbilityContainer {
        require(file !in classLoaders) { "Already registered file ${file.name}" }

        val classLoader = AbilityClassLoader(this, file, javaClass.classLoader)

        try {
            val abilityClass =
                Class.forName(description.main, true, classLoader).asSubclass(Ability::class.java) //메인 클래스 찾기
            val abilityKClass = abilityClass.kotlin
            val conceptClassName =
                abilityKClass.supertypes.first().arguments.first().type.toString().removePrefix("class ")
            val conceptClass = Class.forName(conceptClassName, true, classLoader).asSubclass(AbilityConcept::class.java)

            testCreateInstance(abilityClass)
            testCreateInstance(conceptClass)

            classLoaders[file] = classLoader // 글로벌 클래스 로더 등록

            return AbilityContainer(file, description, conceptClass, abilityClass)
        } catch (e: Exception) {
            classLoader.close()
            throw e
        }
    }

    @Throws(ClassNotFoundException::class)
    internal fun findClass(name: String, skip: AbilityClassLoader): Class<*> {
        var found = classes[name]

        if (found != null) return found

        for (loader in classLoaders.values) {
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

    fun clear() {
        classes.clear()
        classLoaders.run {
            values.forEach(AbilityClassLoader::close)
            clear()
        }
    }
}

private fun <T> testCreateInstance(clazz: Class<T>): T {
    try {
        return clazz.newInstance()
    } catch (e: Exception) {
        error("Failed to create instance ${clazz.name}")
    }
}