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

package com.github.noonmaru.psychics.util

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import javax.annotation.Nonnull

class UpstreamReference<T> : WeakReference<T> {
    constructor(referent: T) : super(referent)
    constructor(
        referent: T, q: ReferenceQueue<in T>?
    ) : super(referent, q)

    @Nonnull
    override fun get(): T {
        return super.get()
            ?: throw IllegalStateException("Cannot get reference as it has already been Garbage Collected")
    }

    override fun hashCode(): Int {
        return get().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return get() == other
    }

    override fun toString(): String {
        return get().toString()
    }
}