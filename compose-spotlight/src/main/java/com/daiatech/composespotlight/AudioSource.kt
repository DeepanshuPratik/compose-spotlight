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

/**
 * Sealed class representing different audio source types that can be used with spotlight messages.
 *
 * This abstraction allows for flexible audio input while maintaining type safety.
 * Audio can come from URIs, raw resources, file paths, or be explicitly disabled.
 */
sealed class AudioSource {
    /**
     * Audio source from a URI string.
     *
     * @property uri The URI string pointing to the audio resource
     *
     * Example:
     * ```kotlin
     * AudioSource.Uri("android.resource://com.example.app/2130968576")
     * AudioSource.Uri("file:///sdcard/audio.mp3")
     * AudioSource.Uri("https://example.com/audio.mp3")
     * ```
     */
    data class Uri(val uri: String) : AudioSource()

    /**
     * Audio source from a raw resource ID.
     * Automatically converts the resource ID to an Android resource URI.
     *
     * @property context Android context used to resolve the package name
     * @property resId The raw resource ID (e.g., R.raw.audio_file)
     *
     * Example:
     * ```kotlin
     * AudioSource.Resource(context, R.raw.welcome_audio)
     * ```
     */
    data class Resource(val context: Context, @RawRes val resId: Int) : AudioSource() {
        /**
         * The generated URI for this resource.
         */
        val uri: String get() = "android.resource://${context.packageName}/$resId"
    }

    /**
     * Audio source from a file path.
     * Automatically prepends the "file://" scheme if not present.
     *
     * @property path The file system path to the audio file
     *
     * Example:
     * ```kotlin
     * AudioSource.File("/data/data/com.example.app/files/audio.mp3")
     * ```
     */
    data class File(val path: String) : AudioSource() {
        /**
         * The generated URI for this file path.
         */
        val uri: String get() = if (path.startsWith("file://")) path else "file://$path"
    }

    /**
     * Represents the absence of audio.
     * Use this to explicitly create silent spotlight messages.
     *
     * Example:
     * ```kotlin
     * AudioSource.None
     * ```
     */
    data object None : AudioSource()
}

/**
 * Extension function on [SpotlightMessageBuilder] to set audio from an [AudioSource].
 *
 * This provides a unified way to configure audio regardless of the source type.
 *
 * Example:
 * ```kotlin
 * spotlightMessage {
 *     content { Text("Welcome") }
 *     audio(AudioSource.Resource(context, R.raw.welcome))
 * }
 *
 * spotlightMessage {
 *     content { Text("Silent tip") }
 *     audio(AudioSource.None)
 * }
 * ```
 *
 * @param source The audio source to use for this message
 * @return This builder instance for chaining
 */
fun SpotlightMessageBuilder.audio(source: AudioSource) = apply {
    when (source) {
        is AudioSource.Uri -> audioUri(source.uri)
        is AudioSource.Resource -> audioUri(source.uri)
        is AudioSource.File -> audioUri(source.uri)
        AudioSource.None -> disableAudio()
    }
}
