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

import androidx.compose.ui.graphics.Color

/**
 * Defines the visual effect shown in the dimming overlay around the spotlighted component.
 *
 * Pass an instance of this sealed class to [DimmingGround] via the `effect` parameter
 * to control which animation is rendered:
 *
 * - [Ripple]: expanding water-ripple rings radiating outward from the spotlight center.
 * - [HandGesture]: an animated pointing-hand icon that bobs toward the spotlight,
 *   indicating where the user should tap.
 */
sealed class SpotlightEffect {

    /**
     * Expanding water-ripple rings that radiate outward from the spotlight center.
     *
     * @param intensity Controls ring visibility.
     *   `0f` produces a smooth gradient with no visible rings.
     *   `1f` produces maximum contrast between ring peaks and troughs.
     *   Default: [SpotlightDefaults.RippleIntensity].
     * @param color Color used for the dimming overlay and ripple rings.
     *   Default: [SpotlightDefaults.RippleColor].
     * @param animated Whether rings animate continuously outward.
     *   When `false`, a static gradient is rendered instead.
     *   Default: [SpotlightDefaults.RippleAnimated].
     * @param speedMs Duration in milliseconds for one full expansion cycle.
     *   Lower values produce faster ripples; higher values produce slower ones.
     *   Default: [SpotlightDefaults.RippleSpeedMs].
     */
    data class Ripple(
        val intensity: Float = SpotlightDefaults.RippleIntensity,
        val color: Color = SpotlightDefaults.RippleColor,
        val animated: Boolean = SpotlightDefaults.RippleAnimated,
        val speedMs: Int = SpotlightDefaults.RippleSpeedMs,
    ) : SpotlightEffect()

    /**
     * A Lottie-based tap-nudge animation that shows a hand tapping toward the spotlight
     * target, with an expanding ripple at the contact point.
     *
     * The hand is positioned above the spotlight cutout. At peak tap its fingertip
     * aligns with the spotlight edge; then it slowly returns and repeats.
     *
     * @param color Color of the dimming overlay background.
     *   Default: [SpotlightDefaults.HandGestureColor] (black).
     * @param speedMs Duration in milliseconds for one full tap-and-return cycle.
     *   Lower values produce a faster tap; higher values slow it down.
     *   The Lottie animation's natural speed is ~2020ms — values below that speed
     *   it up, values above slow it down.
     *   Default: [SpotlightDefaults.HandGestureSpeedMs].
     */
    data class HandGesture(
        val color: Color = SpotlightDefaults.HandGestureColor,
        val speedMs: Int = SpotlightDefaults.HandGestureSpeedMs,
    ) : SpotlightEffect()
}
