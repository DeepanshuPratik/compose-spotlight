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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daiatech.composespotlight.internal.DimmingGroundImpl

/**
 * A composable that provides a spotlight playground for spotlighting different zones.
 *
 * @param controller The spotlight controller to use.
 * @param modifier The modifier to be applied to the spotlight ground.
 * @param rippleIntensity Controls the visibility of the ripple rings in the dimming overlay.
 *   `0f` produces a smooth gradient with no visible rings.
 *   `1f` produces maximum contrast between ring peaks and troughs.
 *   Default is [SpotlightDefaults.RippleIntensity].
 * @param rippleColor The color used for the dimming overlay and ripple rings.
 *   Default is [SpotlightDefaults.RippleColor] (black).
 * @param content The content of the spotlight ground.
 */
@Composable
fun DimmingGround(
    controller: SpotlightController,
    modifier: Modifier = Modifier,
    rippleIntensity: Float = SpotlightDefaults.RippleIntensity,
    rippleColor: Color = SpotlightDefaults.RippleColor,
    content: @Composable () -> Unit
) {
    val currentSpotlightZone by controller.zoneLocationState.collectAsStateWithLifecycle()
    val dimState by controller.dimState.collectAsStateWithLifecycle()

    currentSpotlightZone.let {
        DimmingGroundImpl(
            dimState = dimState,
            position = it.offset,
            size = it.size,
            shape = it.shape,
            rippleIntensity = rippleIntensity.coerceIn(0f, 1f),
            rippleColor = rippleColor,
            modifier = modifier,
            content = content
        )
    }
}
