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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
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
    forcedNavigation: Boolean = false,
    adaptComponentShape: Boolean = false,
    spotlightPadding: Dp = SpotlightDefaults.SpotlightPadding,
    modifier: Modifier = Modifier,
    rippleIntensity: Float = SpotlightDefaults.RippleIntensity,
    rippleColor: Color = SpotlightDefaults.RippleColor,
    rippleAnimated: Boolean = SpotlightDefaults.RippleAnimated,
    rippleSpeedMs: Int = SpotlightDefaults.RippleSpeedMs,
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
            adaptComponentShape = adaptComponentShape,
            spotlightPadding = spotlightPadding,
            rippleIntensity = rippleIntensity,
            rippleColor = rippleColor,
            rippleAnimated = rippleAnimated,
            rippleSpeedMs = rippleSpeedMs
        )
        // When forcedNavigation is active, block all touches outside the spotlight zone.
        // Uses 4 separate touch-blocking regions (top, bottom, left, right) arranged
        // around the spotlight zone, leaving a hole so touches reach the content below.
        if (dimState == DimState.RUNNING && forcedNavigation) {
            TouchBlockerWithHole(
                holePosition = position,
                holeSize = size,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Lays out 4 touch-consuming rectangles around a hole defined by [holePosition] and [holeSize].
 *
 * ```
 * ┌──────────────────────┐
 * │      TOP STRIP       │  (full width, from y=0 to hole top)
 * ├────┬────────────┬────┤
 * │LEFT│   (hole)   │RGHT│  (left/right strips beside the hole)
 * ├────┴────────────┴────┤
 * │     BOTTOM STRIP     │  (full width, from hole bottom to screen bottom)
 * └──────────────────────┘
 * ```
 *
 * The hole area has no overlay, so touches there pass through to the content below.
 */
@Composable
private fun TouchBlockerWithHole(
    holePosition: IntOffset,
    holeSize: IntSize,
    modifier: Modifier = Modifier
) {
    val consumeTouches = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            }
        }
    }

    Layout(
        content = {
            // 0: Top strip
            Box(consumeTouches)
            // 1: Bottom strip
            Box(consumeTouches)
            // 2: Left strip
            Box(consumeTouches)
            // 3: Right strip
            Box(consumeTouches)
        },
        modifier = modifier
    ) { measurables, constraints ->
        val totalW = constraints.maxWidth
        val totalH = constraints.maxHeight

        val holeLeft = holePosition.x.coerceIn(0, totalW)
        val holeTop = holePosition.y.coerceIn(0, totalH)
        val holeRight = (holePosition.x + holeSize.width).coerceIn(0, totalW)
        val holeBottom = (holePosition.y + holeSize.height).coerceIn(0, totalH)

        val topH = holeTop
        val bottomH = totalH - holeBottom
        val midH = holeBottom - holeTop
        val leftW = holeLeft
        val rightW = totalW - holeRight

        val topPlaceable = measurables[0].measure(Constraints.fixed(totalW, topH.coerceAtLeast(0)))
        val bottomPlaceable = measurables[1].measure(Constraints.fixed(totalW, bottomH.coerceAtLeast(0)))
        val leftPlaceable = measurables[2].measure(Constraints.fixed(leftW.coerceAtLeast(0), midH.coerceAtLeast(0)))
        val rightPlaceable = measurables[3].measure(Constraints.fixed(rightW.coerceAtLeast(0), midH.coerceAtLeast(0)))

        layout(totalW, totalH) {
            topPlaceable.place(0, 0)
            bottomPlaceable.place(0, holeBottom)
            leftPlaceable.place(0, holeTop)
            rightPlaceable.place(holeRight, holeTop)
        }
    }
}

/**
 * Builds the ripple gradient color stops with expanding rings.
 *
 * Each ring has a lifecycle (0 = born at spotlight center, 1 = faded at outer edge).
 * The 4 rings are evenly staggered so that as [animationProgress] cycles 0→1,
 * rings continuously expand outward like water ripples.
 *
 * [intensity] scales the peak/trough contrast (0f → no visible rings, 1f → maximum).
 * The ripple [color] is used throughout; the outer dim uses the same color for continuity.
 */
private fun buildRippleColorStops(
    r: Float,
    s: Float,
    dimAlpha: Float,
    intensity: Float,
    color: Color,
    animationProgress: Float = 0f
): Array<Pair<Float, Color>> {
    data class Ring(val peakOffset: Float, val troughOffset: Float)

    val rings = listOf(
        Ring(peakOffset = 0.08f, troughOffset = 0.20f),
        Ring(peakOffset = 0.14f, troughOffset = 0.28f),
        Ring(peakOffset = 0.16f, troughOffset = 0.30f),
        Ring(peakOffset = 0.12f, troughOffset = 0.22f),
    )

    val numRings = rings.size
    val ringWidth = 0.10f

    return buildList {
        add(0.0f to Color.Transparent)
        add(r to Color.Transparent)

        for ((index, ring) in rings.withIndex()) {
            // Each ring's lifecycle, evenly staggered (0 = center, 1 = edge)
            val lifecycle = (animationProgress + index.toFloat() / numRings) % 1f

            // Map lifecycle to gradient position (0.05..0.90)
            val peakPos = 0.05f + lifecycle * 0.85f
            val troughPos = (peakPos + ringWidth).coerceAtMost(0.93f)

            // Fade in as ring leaves center, fade out as it reaches the edge
            val fadeIn = (lifecycle / 0.15f).coerceAtMost(1f)
            val fadeOut = ((1f - lifecycle) / 0.15f).coerceAtMost(1f)
            val visibility = intensity * fadeIn * fadeOut

            val baselinePeak = dimAlpha * peakPos
            val baselineTrough = dimAlpha * troughPos

            val peakA = baselinePeak + ring.peakOffset * visibility
            val troughA = (baselineTrough - ring.troughOffset * visibility).coerceAtLeast(0f)

            add(r + s * peakPos to color.copy(alpha = peakA))
            add(r + s * troughPos to color.copy(alpha = troughA))
        }

        // Continuous settle using the same ripple color — no black transition
        add(r + s * 0.94f to color.copy(alpha = dimAlpha * 0.88f))
        add(1.0f to color.copy(alpha = dimAlpha))
    }
    .sortedBy { it.first }
    .toTypedArray()
}

/**
 * Linearly interpolates a color from an array of color stops at the given [position] (0..1).
 */
private fun sampleColorStops(
    stops: Array<Pair<Float, Color>>,
    position: Float
): Color {
    if (stops.isEmpty()) return Color.Transparent
    if (position <= stops.first().first) return stops.first().second
    if (position >= stops.last().first) return stops.last().second
    for (i in 0 until stops.size - 1) {
        val (pos0, col0) = stops[i]
        val (pos1, col1) = stops[i + 1]
        if (position in pos0..pos1) {
            val t = if (pos1 == pos0) 0f else (position - pos0) / (pos1 - pos0)
            return lerp(col0, col1, t)
        }
    }
    return stops.last().second
}

/**
 * Linearly interpolates between two [Color] values.
 */
private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

@Composable
internal fun DimOverlay(
    modifier: Modifier,
    highlightShape: Shape,
    position: IntOffset,
    componentSize: IntSize,
    dimState: DimState,
    adaptComponentShape: Boolean = false,
    spotlightPadding: Dp = SpotlightDefaults.SpotlightPadding,
    rippleIntensity: Float = SpotlightDefaults.RippleIntensity,
    rippleColor: Color = SpotlightDefaults.RippleColor,
    rippleAnimated: Boolean = SpotlightDefaults.RippleAnimated,
    rippleSpeedMs: Int = SpotlightDefaults.RippleSpeedMs,
    textBoxCornerRadius: Dp = 8.dp
) {
    val objectHighlightWidth = componentSize.width
    val objectHighlightHeight = componentSize.height
    val highlightXCoordinate = position.x.toFloat()
    val highlightYCoordinate = position.y.toFloat()

    Box(Modifier.fillMaxSize()) {
        if (dimState == DimState.RUNNING) {
            val animationProgress = if (rippleAnimated) {
                val infiniteTransition = rememberInfiniteTransition(label = "ripple")
                val progress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = rippleSpeedMs.coerceAtLeast(200),
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rippleWave"
                )
                progress
            } else {
                0f
            }

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
                    } + spotlightPadding.toPx()

                    val rippleExtent = clearRadius * 2.5f
                    val gradientRadius = clearRadius + rippleExtent

                    val dimAlpha = 0.5f

                    val r = clearRadius / gradientRadius
                    val s = 1f - r

                    val colorStops = buildRippleColorStops(r, s, dimAlpha, rippleIntensity, rippleColor, animationProgress)

                    if (adaptComponentShape) {
                        // Shape-adapted ripple: concentric scaled versions of the component shape
                        val padding = spotlightPadding.toPx()
                        val baseW = objectHighlightWidth.toFloat() + padding * 2
                        val baseH = objectHighlightHeight.toFloat() + padding * 2

                        // Layer 1: fill with outermost dim color as base
                        drawRect(color = rippleColor.copy(alpha = dimAlpha))

                        // Layer 2: draw ~40 concentric scaled shapes from outermost to innermost
                        val layerCount = 40
                        val maxScale = gradientRadius / (clearRadius.coerceAtLeast(1f))

                        for (i in 0 until layerCount) {
                            // t goes from 1.0 (outermost) to 0.0 (innermost, at cutout edge)
                            val t = 1f - i.toFloat() / (layerCount - 1).toFloat()
                            val scale = 1f + (maxScale - 1f) * t

                            val scaledW = baseW * scale
                            val scaledH = baseH * scale
                            val scaledSize = Size(scaledW, scaledH)

                            val layerOutline = highlightShape.createOutline(
                                size = scaledSize,
                                layoutDirection = layoutDirection,
                                density = this
                            )
                            val layerPath = Path().apply {
                                when (layerOutline) {
                                    is Outline.Rectangle -> addRect(layerOutline.rect)
                                    is Outline.Rounded -> addRoundRect(layerOutline.roundRect)
                                    is Outline.Generic -> addPath(layerOutline.path)
                                }
                            }

                            // Map scale position to gradient position (r..1)
                            val gradientPos = r + s * t
                            val layerColor = sampleColorStops(colorStops, gradientPos)

                            val offsetX = cx - scaledW / 2f
                            val offsetY = cy - scaledH / 2f
                            translate(left = offsetX, top = offsetY) {
                                drawPath(layerPath, layerColor, blendMode = BlendMode.Src)
                            }
                        }

                        // Layer 3: clear the cutout shape
                        val cutoutSize = Size(baseW, baseH)
                        val cutoutOutline = highlightShape.createOutline(
                            size = cutoutSize,
                            layoutDirection = layoutDirection,
                            density = this
                        )
                        val cutoutPath = Path().apply {
                            when (cutoutOutline) {
                                is Outline.Rectangle -> addRect(cutoutOutline.rect)
                                is Outline.Rounded -> addRoundRect(cutoutOutline.roundRect)
                                is Outline.Generic -> addPath(cutoutOutline.path)
                            }
                        }
                        translate(
                            left = highlightXCoordinate - padding,
                            top = highlightYCoordinate - padding
                        ) {
                            drawPath(cutoutPath, Color.Black, blendMode = BlendMode.Clear)
                        }
                    } else {
                        // Default: radial gradient ripple
                        val rippleBrush = Brush.radialGradient(
                            colorStops = colorStops,
                            center = center,
                            radius = gradientRadius,
                            tileMode = TileMode.Clamp
                        )

                        // Layer 1: draw the ripple gradient over the full screen
                        drawRect(brush = rippleBrush)

                        // Layer 2: punch out the exact shape of the spotlight target
                        val padding = spotlightPadding.toPx()
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
                }
            )
        }
    }
}
