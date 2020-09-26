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

package com.github.noonmaru.psychics.format

import java.text.DecimalFormat


private val DECIMAL_FORMAT = DecimalFormat("#.##")
private val PERCENT_FORMAT = DecimalFormat("#.##%")

internal fun Number.decimalFormat(): String {
    return DECIMAL_FORMAT.format(this)
}

internal fun Number.percentFormat(): String {
    return PERCENT_FORMAT.format(this)
}