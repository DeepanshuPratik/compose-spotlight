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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.composespotlight.models.DimState
import com.daiatech.composespotlight.models.SpotlightLocation
import com.daiatech.composespotlight.models.SpotlightZoneData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for controlling spotlighting of different zones on a [DimmingGround].
 */
@OptIn(ExperimentalMaterial3Api::class)
interface SpotlightController {
    val dimState: StateFlow<DimState>
    val zoneLocationState: StateFlow<SpotlightLocation>
    val currentZoneKey: String?
    val currentTooltipState: TooltipState?
    val currentAudioPlayer: ExoPlayer?

    /**
     * Setup the [SpotlightController] with an id.
     * The first method to be called before using the controller.
     */
    suspend fun setup(id: String)

    /**
     * Checks if the spotlighting queue is persistent.
     */
    suspend fun isPersistent(): Boolean

    /**
     * Makes the spotlighting queue persistent.
     * The queue will be saved in the disk and restored when the controller is setup again.
     * No more [enqueue] and [enqueueAll] calls will be consumed once this is called.
     */
    suspend fun setPersistent()

    /**
     * Dims the [DimmingGround].
     */
    fun startGroundDimming()

    /**
     * Stops dimming the [DimmingGround].
     */
    fun stopGroundDimming()

    /**
     * Enqueues a [SpotlightZone] to be spotlighted to the spotlighting queue.
     */
    suspend fun enqueue(key: String)

    /**
     * Enqueues a list of [SpotlightZone] to be spotlighted to the spotlighting queue.
     */
    suspend fun enqueueAll(keys: List<String>)

    /**
     * Dequeues a [SpotlightZone] from the spotlighting queue, and then spotlights it.
     */
    suspend fun dequeueAndSpotlight(groundDimming: Boolean = true)

    /**
     * Register a new spotlight zone with the provided key and zone data to the controller.
     */
    fun registerZone(key: String, zoneData: SpotlightZoneData)

    /**
     * Unregisters a spotlight zone with the provided key, removing it from the controller.
     */
    fun unregisterZone(key: String)

    /**
     * Checks if the audio player is playing.
     */
    fun isAudioPlaying(): Flow<Boolean>
}

/**
 * Configuration class for setting up a SpotlightController.
 *
 * This provides a declarative way to configure controller behavior including
 * persistence, initial queue state, and other settings.
 *
 * Example:
 * ```kotlin
 * val config = SpotlightControllerConfig("onboarding").apply {
 *     persistent = true
 *     initialQueue = listOf("welcome", "profile")
 * }
 * ```
 */
class SpotlightControllerConfig(
    /**
     * Unique identifier for this controller
     */
    val id: String
) {
    /**
     * Whether the spotlight queue should persist across app restarts
     */
    var persistent: Boolean = false

    /**
     * Initial list of zone keys to add to the spotlight queue
     */
    var initialQueue: List<String> = emptyList()

    /**
     * Whether to automatically dim the ground when spotlighting begins
     */
    var autoDim: Boolean = true
}

/**
 * Sets up the SpotlightController with the provided configuration.
 *
 * This is a convenience extension function that applies all configuration settings
 * in the correct order.
 *
 * @param config The configuration to apply to this controller
 *
 * Example:
 * ```kotlin
 * controller.setupWith(
 *     SpotlightControllerConfig(
 *         id = "onboarding",
 *         persistent = true,
 *         initialQueue = listOf("welcome", "profile", "settings")
 *     )
 * )
 * ```
 */
suspend fun SpotlightController.setupWith(config: SpotlightControllerConfig) {
    setup(config.id)
    if (config.persistent) {
        setPersistent()
    }
    if (config.initialQueue.isNotEmpty()) {
        enqueueAll(config.initialQueue)
    }
}

/**
 * DSL function for configuring a SpotlightController with a builder-style API.
 *
 * This provides a more readable and flexible way to set up the controller.
 *
 * @param id Unique identifier for this controller
 * @param block Configuration lambda applied to SpotlightControllerConfig
 *
 * Example:
 * ```kotlin
 * // Simple setup
 * controller.configure("onboarding_tour")
 *
 * // With options
 * controller.configure("dashboard") {
 *     persistent = true
 *     initialQueue = listOf("profile", "settings", "help")
 *     autoDim = true
 * }
 * ```
 */
suspend fun SpotlightController.configure(
    id: String,
    block: SpotlightControllerConfig.() -> Unit = {}
) {
    val config = SpotlightControllerConfig(id).apply(block)
    setupWith(config)
}
