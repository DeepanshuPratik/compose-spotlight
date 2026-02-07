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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.daiatech.composespotlight.SpotlightDefaults
import com.daiatech.composespotlight.models.DimState
import kotlin.math.sqrt

@Composable
internal fun DimmingGroundImpl(
    dimState: DimState,
    position: IntOffset,
    size: IntSize,
    shape: Shape,
    modifier: Modifier = Modifier,
    rippleIntensity: Float = SpotlightDefaults.RippleIntensity,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
        DimOverlay(
            modifier = Modifier.fillMaxSize(),
            highlightShape = shape,
            position = position,
            componentSize = size,
            dimState = dimState,
            rippleIntensity = rippleIntensity
        )
    }
}

/**
 * Builds the ripple gradient color stops.
 *
 * [intensity] controls **how many rings** are visible (0f → 0 rings, 1f → 4 rings).
 * Inactive rings collapse into the smooth gradient baseline (`dimAlpha * position`).
 * Each ring fades in gradually as the intensity crosses its threshold.
 *
 * @param r         ratio (0‑1) where the fully‑clear zone ends inside the gradient.
 * @param s         remaining ratio (`1 - r`) available for the ripple zone.
 * @param dimAlpha  maximum overlay alpha at full dim.
 * @param intensity clamped 0‑1 ripple intensity.
 */
private fun buildRippleColorStops(
    r: Float,
    s: Float,
    dimAlpha: Float,
    intensity: Float
): Array<Pair<Float, Color>> {
    val dim = Color.Black

    // Each ring is defined by its position in the ripple zone and how far
    // it deviates from the smooth baseline when fully visible.
    data class Ring(
        val peakPos: Float,      // position in ripple zone [0, 1]
        val troughPos: Float,    // position in ripple zone [0, 1]
        val peakOffset: Float,   // added to baseline alpha at peak
        val troughOffset: Float  // subtracted from baseline alpha at trough
    )

    val rings = listOf(
        Ring(peakPos = 0.12f, troughPos = 0.22f, peakOffset = 0.02f, troughOffset = 0.06f),
        Ring(peakPos = 0.32f, troughPos = 0.44f, peakOffset = 0.04f, troughOffset = 0.09f),
        Ring(peakPos = 0.55f, troughPos = 0.67f, peakOffset = 0.05f, troughOffset = 0.10f),
        Ring(peakPos = 0.78f, troughPos = 0.88f, peakOffset = 0.04f, troughOffset = 0.07f),
    )

    // intensity 0 → 0 active rings, intensity 1 → all 4 active
    val activeRings = intensity * rings.size

    return buildList {
        // Fully clear spotlight zone
        add(0.0f to Color.Transparent)
        add(r to Color.Transparent)

        for ((index, ring) in rings.withIndex()) {
            // 0 = ring collapsed into smooth gradient, 1 = fully visible wave
            val visibility = (activeRings - index).coerceIn(0f, 1f)

            // Smooth gradient baseline: alpha increases linearly with position
            val baselinePeak = dimAlpha * ring.peakPos
            val baselineTrough = dimAlpha * ring.troughPos

            // Visible ring: peak rises above baseline, trough dips below it
            val peakA = baselinePeak + ring.peakOffset * visibility
            val troughA = (baselineTrough - ring.troughOffset * visibility).coerceAtLeast(0f)

            add(r + s * ring.peakPos to dim.copy(alpha = peakA))
            add(r + s * ring.troughPos to dim.copy(alpha = troughA))
        }

        // Settle to full dim
        add(r + s * 0.95f to dim.copy(alpha = dimAlpha * 0.85f))
        add(1.0f to dim.copy(alpha = dimAlpha))
    }.toTypedArray()
}

@Composable
internal fun DimOverlay(
    modifier: Modifier,
    highlightShape: Shape,
    position: IntOffset,
    componentSize: IntSize,
    dimState: DimState,
    rippleIntensity: Float = SpotlightDefaults.RippleIntensity,
    textBoxCornerRadius: Dp = 8.dp
) {
    val objectHighlightWidth = componentSize.width
    val objectHighlightHeight = componentSize.height
    val highlightXCoordinate = position.x.toFloat()
    val highlightYCoordinate = position.y.toFloat()

    Box(Modifier.fillMaxSize()) {
        if (dimState == DimState.RUNNING) {
            Canvas(
                modifier = modifier.fillMaxSize(),
                onDraw = {
                    val cx = highlightXCoordinate + objectHighlightWidth / 2f
                    val cy = highlightYCoordinate + objectHighlightHeight / 2f
                    val center = Offset(cx, cy)

                    // Clear zone radius — fully transparent area around the spotlight target
                    val clearRadius = when (highlightShape) {
                        CircleShape -> objectHighlightWidth / 2f
                        else -> sqrt(
                            (objectHighlightWidth / 2f).let { it * it } +
                            (objectHighlightHeight / 2f).let { it * it }
                        )
                    } + 4.dp.toPx()

                    // Ripple zone extends outward from the clear zone edge
                    val rippleExtent = clearRadius * 2.5f
                    val gradientRadius = clearRadius + rippleExtent

                    val dimAlpha = 0.5f

                    // Ratio where the fully clear spotlight zone ends
                    val r = clearRadius / gradientRadius
                    val s = 1f - r

                    val colorStops = buildRippleColorStops(r, s, dimAlpha, rippleIntensity)

                    val rippleBrush = Brush.radialGradient(
                        colorStops = colorStops,
                        center = center,
                        radius = gradientRadius,
                        tileMode = TileMode.Clamp
                    )

                    drawRect(brush = rippleBrush)
                }
            )
        }
    }
}
