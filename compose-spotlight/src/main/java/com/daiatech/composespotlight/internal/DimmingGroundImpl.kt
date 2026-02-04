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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import com.daiatech.composespotlight.models.DimState

@Composable
internal fun DimmingGroundImpl(
    dimState: DimState,
    position: IntOffset,
    size: IntSize,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        content()
        DimOverlay(
            modifier = Modifier.fillMaxSize(),
            highlightShape = shape,
            position = position,
            componentSize = size,
            dimState = dimState
        )
    }
}

@Composable
internal fun DimOverlay(
    modifier: Modifier,
    highlightShape: Shape,
    position: IntOffset,
    componentSize: IntSize,
    dimState: DimState,
    textBoxCornerRadius: Dp = 8.dp
) {
    /** Highlighted Object Spec **/
    val objectHighlightWidth = componentSize.width // Width of the target component
    val objectHighlightHeight = componentSize.height // Height of the target component
    val highlightXCoordinate = position.x.toFloat() // X-coordinate of the target component
    val highlightYCoordinate = position.y.toFloat() // Y-coordinate of the target component

    Box(Modifier.fillMaxSize()) {
        /** Highlighting component **/
        if (dimState == DimState.RUNNING) {
            Canvas(
                modifier = modifier.fillMaxSize(),
                onDraw = {
                    val spotlightPath = Path().apply {
                        when (highlightShape) {
                            RectangleShape -> {
                                addRoundRect(
                                    roundRect = RoundRect(
                                        left = highlightXCoordinate,
                                        top = highlightYCoordinate,
                                        right = highlightXCoordinate + objectHighlightWidth,
                                        bottom = highlightYCoordinate + objectHighlightHeight,
                                        radiusX = textBoxCornerRadius.toPx(),
                                        radiusY = textBoxCornerRadius.toPx()
                                    )
                                )
                            }

                            CircleShape -> {
                                addOval(
                                    oval = Rect(
                                        center = position.toOffset()
                                            .copy(
                                                x = highlightXCoordinate + objectHighlightWidth / 2,
                                                y = highlightYCoordinate + objectHighlightHeight / 2
                                            ),
                                        radius = (componentSize.width.toFloat() / 2)
                                    )
                                )
                            }
                        }
                    }
                    clipPath(
                        path = spotlightPath,
                        clipOp = ClipOp.Difference
                    ) {
                        drawRect(
                            SolidColor(
                                Color.Black.copy(
                                    alpha = 0.5f
                                )
                            )
                        )
                    }
                }
            )
        }
    }
}
