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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
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
    rippleColor: Color = SpotlightDefaults.RippleColor,
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
            rippleIntensity = rippleIntensity,
            rippleColor = rippleColor
        )
    }
}

/**
 * Builds the ripple gradient color stops.
 *
 * [intensity] controls **how many rings** are visible (0f → 0 rings, 1f → 4 rings).
 * Inactive rings collapse into the smooth gradient baseline (`dimAlpha * position`).
 * The ripple [color] is used throughout; the outer dim uses the same color for continuity.
 */
private fun buildRippleColorStops(
    r: Float,
    s: Float,
    dimAlpha: Float,
    intensity: Float,
    color: Color
): Array<Pair<Float, Color>> {
    data class Ring(
        val peakPos: Float,
        val troughPos: Float,
        val peakOffset: Float,
        val troughOffset: Float
    )

    val rings = listOf(
        Ring(peakPos = 0.12f, troughPos = 0.22f, peakOffset = 0.02f, troughOffset = 0.06f),
        Ring(peakPos = 0.32f, troughPos = 0.44f, peakOffset = 0.04f, troughOffset = 0.09f),
        Ring(peakPos = 0.55f, troughPos = 0.67f, peakOffset = 0.05f, troughOffset = 0.10f),
        Ring(peakPos = 0.78f, troughPos = 0.88f, peakOffset = 0.04f, troughOffset = 0.07f),
    )

    val activeRings = intensity * rings.size

    return buildList {
        add(0.0f to Color.Transparent)
        add(r to Color.Transparent)

        for ((index, ring) in rings.withIndex()) {
            val visibility = (activeRings - index).coerceIn(0f, 1f)

            val baselinePeak = dimAlpha * ring.peakPos
            val baselineTrough = dimAlpha * ring.troughPos

            val peakA = baselinePeak + ring.peakOffset * visibility
            val troughA = (baselineTrough - ring.troughOffset * visibility).coerceAtLeast(0f)

            add(r + s * ring.peakPos to color.copy(alpha = peakA))
            add(r + s * ring.troughPos to color.copy(alpha = troughA))
        }

        // Continuous settle using the same ripple color — no black transition
        add(r + s * 0.94f to color.copy(alpha = dimAlpha * 0.88f))
        add(1.0f to color.copy(alpha = dimAlpha))
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
    rippleColor: Color = SpotlightDefaults.RippleColor,
    textBoxCornerRadius: Dp = 8.dp
) {
    val objectHighlightWidth = componentSize.width
    val objectHighlightHeight = componentSize.height
    val highlightXCoordinate = position.x.toFloat()
    val highlightYCoordinate = position.y.toFloat()

    Box(Modifier.fillMaxSize()) {
        if (dimState == DimState.RUNNING) {
            Canvas(
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                onDraw = {
                    val cx = highlightXCoordinate + objectHighlightWidth / 2f
                    val cy = highlightYCoordinate + objectHighlightHeight / 2f
                    val center = Offset(cx, cy)

                    val clearRadius = when (highlightShape) {
                        CircleShape -> objectHighlightWidth / 2f
                        else -> sqrt(
                            (objectHighlightWidth / 2f).let { it * it } +
                            (objectHighlightHeight / 2f).let { it * it }
                        )
                    } + 4.dp.toPx()

                    val rippleExtent = clearRadius * 2.5f
                    val gradientRadius = clearRadius + rippleExtent

                    val dimAlpha = 0.5f

                    val r = clearRadius / gradientRadius
                    val s = 1f - r

                    val colorStops = buildRippleColorStops(r, s, dimAlpha, rippleIntensity, rippleColor)

                    val rippleBrush = Brush.radialGradient(
                        colorStops = colorStops,
                        center = center,
                        radius = gradientRadius,
                        tileMode = TileMode.Clamp
                    )

                    // Layer 1: draw the ripple gradient over the full screen
                    drawRect(brush = rippleBrush)

                    // Layer 2: punch out the exact shape of the spotlight target
                    val padding = 4.dp.toPx()
                    val shapeSize = Size(
                        objectHighlightWidth.toFloat() + padding * 2,
                        objectHighlightHeight.toFloat() + padding * 2
                    )
                    val outline = highlightShape.createOutline(
                        size = shapeSize,
                        layoutDirection = layoutDirection,
                        density = this
                    )
                    val spotlightPath = Path().apply {
                        when (outline) {
                            is Outline.Rectangle -> addRect(outline.rect)
                            is Outline.Rounded -> addRoundRect(outline.roundRect)
                            is Outline.Generic -> addPath(outline.path)
                        }
                    }
                    translate(
                        left = highlightXCoordinate - padding,
                        top = highlightYCoordinate - padding
                    ) {
                        drawPath(spotlightPath, Color.Black, blendMode = BlendMode.Clear)
                    }
                }
            )
        }
    }
}
