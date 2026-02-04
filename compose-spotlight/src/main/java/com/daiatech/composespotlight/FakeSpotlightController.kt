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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * A fake implementation of [SpotlightController] for testing purposes.
 * All operations are no-ops and state remains unchanged.
 *
 * Use this in your tests when you need to provide a [SpotlightController]
 * but don't want to test spotlight behavior itself.
 *
 * Example:
 * ```
 * @Test
 * fun testMyScreen() {
 *     val fakeController = FakeSpotlightController()
 *     composeTestRule.setContent {
 *         MyScreen(spotlightController = fakeController)
 *     }
 *     // Your test assertions
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
class FakeSpotlightController : SpotlightController {
    private val _dimState = MutableStateFlow(DimState.STOPPED)
    override val dimState: StateFlow<DimState> = _dimState.asStateFlow()

    private val _zoneLocationState = MutableStateFlow(SpotlightLocation())
    override val zoneLocationState: StateFlow<SpotlightLocation> = _zoneLocationState.asStateFlow()

    override var currentZoneKey: String? = null
        private set

    override val currentTooltipState: TooltipState? = null

    override val currentAudioPlayer: ExoPlayer? = null

    private var isPersistentValue = false
    private val zones = mutableMapOf<String, SpotlightZoneData>()
    private val queue = mutableListOf<String>()

    override suspend fun setup(id: String) {
        // No-op for fake implementation
    }

    override suspend fun isPersistent(): Boolean {
        return isPersistentValue
    }

    override suspend fun setPersistent() {
        isPersistentValue = true
    }

    override fun startGroundDimming() {
        _dimState.value = DimState.RUNNING
    }

    override fun stopGroundDimming() {
        _dimState.value = DimState.STOPPED
    }

    override suspend fun enqueue(key: String) {
        if (!isPersistentValue) {
            queue.add(key)
        }
    }

    override suspend fun enqueueAll(keys: List<String>) {
        if (!isPersistentValue) {
            queue.addAll(keys)
        }
    }

    override suspend fun dequeueAndSpotlight(groundDimming: Boolean) {
        if (queue.isNotEmpty()) {
            currentZoneKey = queue.removeAt(0)
            if (groundDimming) {
                startGroundDimming()
            } else {
                stopGroundDimming()
            }
        } else {
            currentZoneKey = null
            stopGroundDimming()
        }
    }

    override fun registerZone(key: String, zoneData: SpotlightZoneData) {
        zones[key] = zoneData
    }

    override fun unregisterZone(key: String) {
        zones.remove(key)
    }

    override fun isAudioPlaying(): Flow<Boolean> {
        return flow { emit(false) }
    }

    /**
     * Get the current queue for testing purposes
     */
    fun getQueue(): List<String> = queue.toList()

    /**
     * Get registered zones for testing purposes
     */
    fun getRegisteredZones(): Map<String, SpotlightZoneData> = zones.toMap()

    /**
     * Reset the controller to initial state for testing
     */
    fun reset() {
        _dimState.value = DimState.STOPPED
        _zoneLocationState.value = SpotlightLocation()
        currentZoneKey = null
        isPersistentValue = false
        zones.clear()
        queue.clear()
    }
}
