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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Default value store for Spotlight.
 */
object SpotlightDefaults {
    val caretHeight = 8.dp
    val caretWidth = 12.dp
    val tonalElevation = 4.dp
    val shadowElevation = 4.dp
    val SpacingBetweenTooltipAndAnchor = 4.dp
    val TooltipMinHeight = 24.dp
    val TooltipMinWidth = 40.dp
    val PlainTooltipMaxWidth = 200.dp
    val PlainTooltipVerticalPadding = 4.dp
    val PlainTooltipHorizontalPadding = 8.dp
    val PlainTooltipContentPadding =
        PaddingValues(PlainTooltipHorizontalPadding, PlainTooltipVerticalPadding)

    /**
     * Default ripple intensity for the spotlight dimming effect.
     * Range: 0f (no ripple, smooth gradient) to 1f (maximum ripple contrast).
     */
    const val RippleIntensity = 1f

    /**
     * Default color used for the spotlight dimming overlay and ripple rings.
     */
    val RippleColor: Color = Color.Black
}
