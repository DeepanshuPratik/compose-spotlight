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

package com.daiatech.composespotlight.internal

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.composespotlight.SpotlightController
import com.daiatech.composespotlight.models.DimState
import com.daiatech.composespotlight.models.SpotlightLocation
import com.daiatech.composespotlight.models.SpotlightZoneData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.LinkedList
import kotlin.math.roundToInt
import kotlin.properties.Delegates

/**
 * An implementation for [SpotlightController].
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class SpotlightControllerImpl(private val spotlightPreferences: SpotlightPreferences) :
    SpotlightController {
    private var id by Delegates.notNull<String>()
    private val keyPersistence get() = "${id}_persistence"
    private val keyPersistentQueue get() = "${id}_persistent_queue"

    private val mapLock = Any()
    private val queueLock = Any()

    private val spotlightZoneMap = mutableMapOf<String, SpotlightZoneData>()
    private var spotlightingQueue = LinkedList<String>()

    private val _dimState = MutableStateFlow(DimState.STOPPED)
    override val dimState = _dimState.asStateFlow()

    private val _zoneLocationState = MutableStateFlow(SpotlightLocation())
    override val zoneLocationState = _zoneLocationState.asStateFlow()

    override var currentZoneKey: String? = null
        private set
    override var currentTooltipState: TooltipState? = null
        private set
    override var currentAudioPlayer: ExoPlayer? = null
        private set

    override suspend fun setup(id: String) {
        this.id = id
        if (isPersistent()) {
            val list = spotlightPreferences.getStringList(keyPersistentQueue, emptyList())
            synchronized(queueLock) {
                spotlightingQueue = LinkedList(list)
            }
        }
    }

    override suspend fun isPersistent(): Boolean {
        val value = spotlightPreferences.getValue(
            key = keyPersistence,
            defaultValue = false
        ) as Boolean
        return value
    }

    override suspend fun setPersistent() {
        if (isPersistent()) return
        spotlightPreferences.setValue(
            key = keyPersistence,
            value = true
        )
        spotlightPreferences.setStringList(keyPersistentQueue, spotlightingQueue)
    }

    override fun startGroundDimming() {
        _dimState.update { DimState.RUNNING }
    }

    override fun stopGroundDimming() {
        _dimState.update { DimState.STOPPED }
    }

    override suspend fun enqueue(key: String) {
        if (isPersistent()) return
        if (!waitForZoneToRegister(key)) return
        runOnSpotlightingQueue { add(key) }
    }

    override suspend fun enqueueAll(keys: List<String>) {
        if (isPersistent()) return
        keys.forEach {
            enqueue(it)
        }
    }

    override suspend fun dequeueAndSpotlight(groundDimming: Boolean) {
        currentTooltipState?.dismiss()
        currentAudioPlayer?.stop()
        currentTooltipState = null
        currentAudioPlayer = null
        currentZoneKey = null

        val head = runOnSpotlightingQueue { removeFirstOrNull() }

        head?.let {
            if (groundDimming) startGroundDimming() else stopGroundDimming()
            putSpotlightOn(it)
        } ?: run { stopGroundDimming() }
    }

    override fun registerZone(
        key: String,
        zoneData: SpotlightZoneData
    ) {
        runOnSpotlightZoneMap { put(key, zoneData) }
    }

    override fun unregisterZone(key: String) {
        runOnSpotlightZoneMap { remove(key) }
    }

    override fun isAudioPlaying(): Flow<Boolean> {
        return flow {
            while (true) {
                emit(currentAudioPlayer?.isPlaying ?: false)
                delay(100)
            }
        }
    }

    private fun <T> runOnSpotlightZoneMap(block: MutableMap<String, SpotlightZoneData>.() -> T): T {
        synchronized(mapLock) {
            return spotlightZoneMap.block()
        }
    }

    private suspend fun <T> runOnSpotlightingQueue(block: LinkedList<String>.() -> T): T {
        val result = synchronized(queueLock) {
            spotlightingQueue.block()
        }
        if (isPersistent()) {
            spotlightPreferences.setStringList(keyPersistentQueue, spotlightingQueue)
        }
        return result
    }

    private suspend fun waitForZoneToRegister(
        zoneKey: String,
        timeoutMillis: Long = 3000
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (runOnSpotlightZoneMap { containsKey(zoneKey) }) {
                return true
            } else {
                delay(50)
            }
        }
        // Element wasn't registered within the timeout period
        return false
    }

    private suspend fun putSpotlightOn(key: String) {
        val zone = runOnSpotlightZoneMap { get(key) } ?: return
        withContext(Dispatchers.Main) {
            _zoneLocationState.update {
                if (zone.layoutCoordinates.isAttached) {
                    SpotlightLocation(
                        offset = with(zone.layoutCoordinates) {
                            val position = positionInRoot()
                            IntOffset(
                                position.x.roundToInt(),
                                position.y.roundToInt()
                            )
                        },
                        size = IntSize(
                            zone.layoutCoordinates.size.width,
                            zone.layoutCoordinates.size.height
                        ),
                        shape = zone.shape,
                        forcedNavigation = zone.forcedNavigation
                    )
                } else {
                    SpotlightLocation(
                        offset = IntOffset.Companion.Zero,
                        size = IntSize.Companion.Zero,
                        shape = zone.shape,
                        forcedNavigation = zone.forcedNavigation
                    )
                }
            }
            currentZoneKey = key
            currentAudioPlayer = zone.audioPlayer
            currentTooltipState = zone.tooltipState
            currentTooltipState?.show()
        }
    }
}
