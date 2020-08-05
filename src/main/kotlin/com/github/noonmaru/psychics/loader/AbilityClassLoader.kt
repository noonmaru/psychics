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

import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

internal class AbilityClassLoader(
    private val loader: AbilityLoader,
    file: File,
    parent: ClassLoader
) :
    URLClassLoader(arrayOf(file.toURI().toURL()), parent) {

    private val classes = ConcurrentHashMap<String, Class<*>>()

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        return try {
            findLocalClass(name)
        } catch (e: ClassNotFoundException) {
            loader.findClass(name, this)
        }
    }

    internal fun findLocalClass(name: String): Class<*> {
        this.classes[name]?.let { return it }

        val found = super.findClass(name)
        this.classes[name] = found

        return found
    }
}