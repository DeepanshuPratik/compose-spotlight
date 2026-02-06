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

package com.daiatech.composespotlight

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import com.daiatech.composespotlight.models.SpotlightMessage

/**
 * Builder class for creating [SpotlightMessage] instances with a flexible, fluent API.
 *
 * Example usage:
 * ```kotlin
 * val message = SpotlightMessageBuilder()
 *     .content { Text("Welcome!") }
 *     .audioResource(context, R.raw.welcome_audio)
 *     .delay(5000L)
 *     .build()
 * ```
 *
 * Or using the DSL function:
 * ```kotlin
 * val message = spotlightMessage {
 *     content { Text("Welcome!") }
 *     audioResource(context, R.raw.welcome_audio)
 *     delay(5000L)
 * }
 * ```
 */
class SpotlightMessageBuilder {
    private var content: (@Composable () -> Unit)? = null
    private var audioUri: String? = null
    private var delayMillis: Long = 3000L
    private var enableAudio: Boolean = true

    /**
     * Sets the composable content to be displayed in the tooltip.
     *
     * @param block The composable function that renders the tooltip content
     * @return This builder instance for chaining
     */
    fun content(block: @Composable () -> Unit) = apply {
        this.content = block
    }

    /**
     * Sets the audio URI to be played when this message is shown.
     *
     * @param uri The URI string pointing to the audio resource
     * @return This builder instance for chaining
     */
    fun audioUri(uri: String) = apply {
        this.audioUri = uri
        this.enableAudio = true
    }

    /**
     * Sets the audio using a raw resource ID.
     * Automatically converts the resource ID to an Android resource URI.
     *
     * @param context Android context used to resolve the package name
     * @param resId The raw resource ID (e.g., R.raw.audio_file)
     * @return This builder instance for chaining
     */
    fun audioResource(context: Context, @RawRes resId: Int) = apply {
        this.audioUri = "android.resource://${context.packageName}/$resId"
        this.enableAudio = true
    }

    /**
     * Sets the delay duration in milliseconds for how long this message should be displayed
     * when no audio is present.
     *
     * @param millis The delay duration in milliseconds (default is 3000ms)
     * @return This builder instance for chaining
     */
    fun delay(millis: Long) = apply {
        this.delayMillis = millis
    }

    /**
     * Disables audio playback for this message.
     * Useful for creating silent messages that rely only on visual content and delay timing.
     *
     * @return This builder instance for chaining
     */
    fun disableAudio() = apply {
        this.enableAudio = false
        this.audioUri = null
    }

    /**
     * Builds and returns the [SpotlightMessage] instance.
     *
     * @return A new [SpotlightMessage] with the configured properties
     * @throws IllegalStateException if content has not been set
     */
    fun build(): SpotlightMessage {
        requireNotNull(content) { "Content must be set using content { } before building" }

        return SpotlightMessage(
            content = content,
            audioFilePath = if (enableAudio) audioUri else null,
            defaultDelayMillis = delayMillis
        )
    }
}

/**
 * DSL function for creating a [SpotlightMessage] using a builder pattern.
 *
 * Example:
 * ```kotlin
 * val message = spotlightMessage {
 *     content { Text("Tap here to continue") }
 *     audioResource(context, R.raw.tap_audio)
 *     delay(4000L)
 * }
 * ```
 *
 * @param block Configuration lambda applied to the builder
 * @return A configured [SpotlightMessage] instance
 */
fun spotlightMessage(block: SpotlightMessageBuilder.() -> Unit): SpotlightMessage {
    return SpotlightMessageBuilder().apply(block).build()
}
