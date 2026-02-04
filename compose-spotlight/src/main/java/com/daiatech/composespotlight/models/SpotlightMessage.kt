/*
 * Copyright 2026 Deepanshu Pratik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.daiatech.composespotlight.models

import androidx.compose.runtime.Composable

/**
 * Represents a message in the spotlight UI that can contain composable content and/or audio.
 *
 * @property content Composable function that renders the visual content of the spotlight message.
 * @property audioFilePath Optional URI pointing to an audio file to be played with this message.
 * @property defaultDelayMillis The default delay in milliseconds before proceeding to the next spotlight message.
 */
data class SpotlightMessage(
    val content: @Composable (() -> Unit)? = null,
    val audioFilePath: String? = null,
    val defaultDelayMillis: Long = 1000
)

enum class SpotlightControlMode {
    MANUAL,
    AUTO
}
