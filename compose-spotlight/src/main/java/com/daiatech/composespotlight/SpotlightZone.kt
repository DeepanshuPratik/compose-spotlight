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

import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.composespotlight.models.SpotlightMessage
import com.daiatech.composespotlight.models.SpotlightZoneData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A composable function that creates a spotlight zone with optional tooltips and audio messages.
 *
 * @param key Unique identifier for this spotlight zone
 * @param modifier Modifier to be applied to the spotlight zone
 * @param messages Optional list of messages to be displayed in the tooltip
 * @param tooltipPosition Vertical position of the tooltip (TOP, BOTTOM, or AUTO for automatic detection)
 * @param tooltipAlignment Horizontal alignment of the tooltip (START, CENTER, END, or AUTO for automatic detection)
 * @param disableTouch Whether touch should be disabled on this spotlight zone
 * @param forcedNavigation When true, only the spotlighted zone is interactive and all other
 *   touches are blocked. Useful for guided onboarding where the user must tap the highlighted element.
 * @param adaptComponentShape When true, the ripple bands follow the contour of the component shape
 *   instead of using a circular radial gradient.
 * @param shape Shape of the spotlight cutout
 * @param caretSize Size of the tooltip caret
 * @param toolTipMaxWidth Maximum width of the tooltip
 * @param controller Controller managing all spotlight zones
 * @param onFinish Callback invoked when the spotlight sequence is completed
 * @param content Composable content to be highlighted by the spotlight
 */
@Composable
fun SpotlightZone(
    key: String,
    modifier: Modifier = Modifier,
    messages: List<SpotlightMessage>? = null,
    tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO,
    disableTouch: Boolean = false,
    forcedNavigation: Boolean = false,
    adaptComponentShape: Boolean = false,
    shape: Shape = RectangleShape,
    caretSize: DpSize = DpSize(SpotlightDefaults.caretHeight, SpotlightDefaults.caretWidth),
    toolTipMaxWidth: Dp = SpotlightDefaults.PlainTooltipMaxWidth,
    controller: SpotlightController,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (LocalInspectionMode.current) {
        Box(modifier) {
            content()
        }
    } else {
        SpotlightZoneCore(
            key = key,
            modifier = modifier,
            messages = messages,
            tooltipPosition = tooltipPosition,
            tooltipAlignment = tooltipAlignment,
            disableTouch = disableTouch,
            forcedNavigation = forcedNavigation,
            adaptComponentShape = adaptComponentShape,
            shape = shape,
            caretSize = caretSize,
            toolTipMaxWidth = toolTipMaxWidth,
            controller = controller,
            onFinish = onFinish,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpotlightZoneCore(
    key: String,
    modifier: Modifier = Modifier,
    messages: List<SpotlightMessage>? = null,
    tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO,
    disableTouch: Boolean = false,
    forcedNavigation: Boolean = false,
    adaptComponentShape: Boolean = false,
    shape: Shape = RectangleShape,
    caretSize: DpSize = DpSize(SpotlightDefaults.caretHeight, SpotlightDefaults.caretWidth),
    toolTipMaxWidth: Dp = SpotlightDefaults.PlainTooltipMaxWidth,
    controller: SpotlightController,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tooltipState = rememberTooltipState(isPersistent = true)
    val positionProvider = rememberTooltipPositionProvider(tooltipPosition, tooltipAlignment)
    var allowTouch = remember { mutableStateOf(true) }
    val isAppInForeground = remember { mutableStateOf(true) }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exoPlayer.playWhenReady = true
                isAppInForeground.value = true
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.playWhenReady = false
                isAppInForeground.value = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    DisposableEffect(key) {
        onDispose {
            controller.unregisterZone(key)
            exoPlayer.release()
        }
    }

    Box(
        modifier.onGloballyPositioned { coordinates ->
            val zone = SpotlightZoneData(
                layoutCoordinates = coordinates,
                tooltipState = tooltipState,
                audioPlayer = exoPlayer,
                shape = shape,
                forcedNavigation = forcedNavigation,
                adaptComponentShape = adaptComponentShape
            )
            controller.registerZone(key, zone)
        }
    ) {
        if (disableTouch && !allowTouch.value) {
            Dialog(onDismissRequest = {}) {
                (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
        if (!messages.isNullOrEmpty()) {
            TooltipBox(
                focusable = false,
                positionProvider = positionProvider,
                tooltip = {
                    var currentMessageIndex by remember { mutableIntStateOf(0) }
                    val messageContent = remember(currentMessageIndex) {
                        messages.getOrNull(currentMessageIndex)?.content ?: messages.last().content
                    }

                    LaunchedEffect(currentMessageIndex) {
                        if (currentMessageIndex > messages.lastIndex) {
                            exoPlayer.playWhenReady = false
                            allowTouch.value = true
                            onFinish.invoke()
                            return@LaunchedEffect
                        }

                        allowTouch.value = false
                        val currentMessage = messages[currentMessageIndex]
                        exoPlayer.playWhenReady = currentMessage.audioFilePath != null && isAppInForeground.value

                        if (currentMessage.audioFilePath == null && isAppInForeground.value) {
                            delay(currentMessage.defaultDelayMillis)
                            currentMessageIndex += 1
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        val mediaItems = messages.mapNotNull {
                            it.audioFilePath?.let { uri ->
                                MediaItem.fromUri(uri)
                            }
                        }
                        val listener = object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                if (currentMessageIndex < messages.lastIndex) {
                                    scope.launch {
                                        exoPlayer.playWhenReady = false
                                        delay(messages[currentMessageIndex].defaultDelayMillis)
                                        exoPlayer.seekTo(currentMessageIndex + 1, 0)
                                        exoPlayer.prepare()
                                    }
                                } else {
                                    allowTouch.value = true
                                    exoPlayer.release()
                                    onFinish.invoke()
                                }
                            }

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int
                            ) {
                                if (exoPlayer.currentMediaItemIndex == 0) return
                                currentMessageIndex += 1
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    currentMessageIndex += 1
                                }
                            }
                        }

                        exoPlayer.addListener(listener)
                        exoPlayer.playWhenReady = false
                        if (mediaItems.isNotEmpty()) {
                            exoPlayer.setMediaItems(mediaItems)
                            exoPlayer.prepare()
                        }

                        onDispose {
                            if (exoPlayer.isPlaying) exoPlayer.stop()
                            exoPlayer.removeListener(listener)
                            exoPlayer.clearMediaItems()
                        }
                    }

                    messageContent?.let {
                        Tooltip(
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                bottom = 8.dp
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentColor = Color.Black,
                            containerColor = Color.White,
                            tonalElevation = SpotlightDefaults.tonalElevation,
                            shadowElevation = SpotlightDefaults.shadowElevation,
                            maxWidth = toolTipMaxWidth
                        ) {
                            it()
                        }
                    }
                },
                state = tooltipState
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun rememberTooltipPositionProvider(
    tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO,
    spacingBetweenTooltipAndAnchor: Dp = 4.dp
): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) {
        spacingBetweenTooltipAndAnchor.roundToPx()
    }
    return remember(tooltipAnchorSpacing, tooltipPosition, tooltipAlignment) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                // --- Vertical positioning ---
                val topY = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                val bottomY = anchorBounds.bottom + tooltipAnchorSpacing

                val y = when (tooltipPosition) {
                    TooltipPosition.TOP -> {
                        if (topY >= 0) topY else bottomY
                    }
                    TooltipPosition.BOTTOM -> {
                        if (bottomY + popupContentSize.height <= windowSize.height) bottomY
                        else topY
                    }
                    TooltipPosition.AUTO -> {
                        val anchorCenterY = anchorBounds.top + anchorBounds.height / 2
                        if (anchorCenterY > windowSize.height / 2) {
                            // Anchor in bottom half → prefer above
                            if (topY >= 0) topY else bottomY
                        } else {
                            // Anchor in top half → prefer below
                            if (bottomY + popupContentSize.height <= windowSize.height) bottomY
                            else topY
                        }
                    }
                }

                // --- Horizontal alignment ---
                val resolvedAlignment = when (tooltipAlignment) {
                    TooltipAlignment.AUTO -> {
                        val anchorCenterX = anchorBounds.left + anchorBounds.width / 2
                        val third = windowSize.width / 3
                        when {
                            anchorCenterX < third -> TooltipAlignment.START
                            anchorCenterX > third * 2 -> TooltipAlignment.END
                            else -> TooltipAlignment.CENTER
                        }
                    }
                    else -> tooltipAlignment
                }

                val x = when (resolvedAlignment) {
                    TooltipAlignment.START -> anchorBounds.left
                    TooltipAlignment.END -> anchorBounds.right - popupContentSize.width
                    TooltipAlignment.CENTER,
                    TooltipAlignment.AUTO -> {
                        anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                    }
                }

                // Clamp to screen bounds
                val clampedX = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val clampedY = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))

                return IntOffset(clampedX, clampedY)
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun TooltipScope.Tooltip(
    modifier: Modifier,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    maxWidth: Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = SpotlightDefaults.TooltipMinWidth,
                    maxWidth = maxWidth,
                    minHeight = SpotlightDefaults.TooltipMinHeight
                )
                .padding(SpotlightDefaults.PlainTooltipContentPadding)
        ) {
            val textStyle = MaterialTheme.typography.bodySmall

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

/**
 * Simplified SpotlightZone composable for displaying a single text message with optional audio.
 *
 * This overload provides a simpler API for the most common use case of showing a simple text message
 * in the spotlight with optional audio from a raw resource.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param message The text message to display in the tooltip
 * @param modifier Modifier to be applied to the spotlight zone
 * @param shape Shape of the spotlight cutout (e.g. CircleShape, RoundedCornerShape, RectangleShape)
 * @param tooltipPosition Vertical position of the tooltip (TOP, BOTTOM, or AUTO for automatic detection)
 * @param tooltipAlignment Horizontal alignment of the tooltip (START, CENTER, END, or AUTO for automatic detection)
 * @param forcedNavigation When true, only this zone is interactive during spotlight
 * @param adaptComponentShape When true, the ripple bands follow the contour of the component shape
 * @param audioResId Optional raw resource ID for audio to play with this message
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "profile",
 *     controller = controller,
 *     message = "Tap here for profile",
 *     shape = RoundedCornerShape(16.dp),
 *     tooltipPosition = TooltipPosition.AUTO,
 *     tooltipAlignment = TooltipAlignment.END,
 *     forcedNavigation = true,
 *     audioResId = R.raw.profile_audio
 * ) {
 *     ProfileButton()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    message: String,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO,
    forcedNavigation: Boolean = false,
    adaptComponentShape: Boolean = false,
    @RawRes audioResId: Int? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val messages = remember(message, audioResId) {
        listOf(
            spotlightMessage {
                content { androidx.compose.material3.Text(message) }
                if (audioResId != null) {
                    audioResource(context, audioResId)
                } else {
                    disableAudio()
                }
            }
        )
    }

    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = messages,
        tooltipPosition = tooltipPosition,
        tooltipAlignment = tooltipAlignment,
        forcedNavigation = forcedNavigation,
        adaptComponentShape = adaptComponentShape,
        shape = shape,
        content = content
    )
}

/**
 * SpotlightZone composable for displaying a single SpotlightMessage.
 *
 * This overload is useful when you want to use a pre-built SpotlightMessage but don't need
 * to specify a list of multiple messages.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param message The SpotlightMessage to display
 * @param modifier Modifier to be applied to the spotlight zone
 * @param tooltipPosition Vertical position of the tooltip (TOP, BOTTOM, or AUTO for automatic detection)
 * @param tooltipAlignment Horizontal alignment of the tooltip (START, CENTER, END, or AUTO for automatic detection)
 * @param shape Shape of the spotlight cutout
 * @param onFinish Callback invoked when the spotlight sequence is completed
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * val message = spotlightMessage {
 *     content { Text("Welcome!") }
 *     audioResource(context, R.raw.welcome)
 * }
 *
 * SpotlightZone(
 *     key = "welcome",
 *     controller = controller,
 *     message = message
 * ) {
 *     WelcomeContent()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    message: SpotlightMessage,
    modifier: Modifier = Modifier,
    tooltipPosition: TooltipPosition = TooltipPosition.AUTO,
    tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO,
    forcedNavigation: Boolean = false,
    adaptComponentShape: Boolean = false,
    shape: Shape = RectangleShape,
    onFinish: () -> Unit = {},
    content: @Composable () -> Unit
) {
    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = listOf(message),
        tooltipPosition = tooltipPosition,
        tooltipAlignment = tooltipAlignment,
        forcedNavigation = forcedNavigation,
        adaptComponentShape = adaptComponentShape,
        shape = shape,
        onFinish = onFinish,
        content = content
    )
}

/**
 * Configuration class for builder-style SpotlightZone setup.
 *
 * This class allows for a more declarative and flexible way to configure spotlight zones
 * with multiple messages and custom settings.
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "tutorial",
 *     controller = controller,
 *     config = {
 *         message {
 *             content { Text("Step 1") }
 *             audioResource(context, R.raw.step1)
 *         }
 *         message {
 *             content { Text("Step 2") }
 *             audioResource(context, R.raw.step2)
 *         }
 *         tooltipPosition = TooltipPosition.BOTTOM
 *         shape = CircleShape
 *         onFinish = { viewModel.completeTutorial() }
 *     }
 * ) {
 *     TutorialContent()
 * }
 * ```
 */
class SpotlightZoneConfig {
    /**
     * List of messages to display in sequence for this spotlight zone.
     */
    var messages: List<SpotlightMessage> = emptyList()
        private set

    /**
     * Vertical position of the tooltip relative to the spotlight zone (TOP, BOTTOM, or AUTO).
     */
    var tooltipPosition: TooltipPosition = TooltipPosition.AUTO

    /**
     * Horizontal alignment of the tooltip relative to the spotlight zone (START, CENTER, END, or AUTO).
     */
    var tooltipAlignment: TooltipAlignment = TooltipAlignment.AUTO

    /**
     * Whether touch input should be disabled when this zone is spotlighted.
     */
    var disableTouch: Boolean = false

    /**
     * When true, only this zone is interactive during spotlight; all other touches are blocked.
     */
    var forcedNavigation: Boolean = false

    /**
     * When true, the ripple bands follow the contour of the component shape.
     */
    var adaptComponentShape: Boolean = false

    /**
     * Shape of the spotlight cutout.
     */
    var shape: Shape = RectangleShape

    /**
     * Callback invoked when all messages have been displayed.
     */
    var onFinish: () -> Unit = {}

    /**
     * Adds a single message using the builder pattern.
     *
     * @param block Configuration lambda for the SpotlightMessageBuilder
     */
    fun message(block: SpotlightMessageBuilder.() -> Unit) {
        messages = messages + spotlightMessage(block)
    }

    /**
     * Sets multiple messages at once.
     *
     * @param msgs Variable number of SpotlightMessage instances
     */
    fun messages(vararg msgs: SpotlightMessage) {
        messages = msgs.toList()
    }
}

/**
 * SpotlightZone composable with builder-style configuration.
 *
 * This overload provides the most flexible API using a configuration DSL that allows
 * for declarative setup of multiple messages and settings.
 *
 * @param key Unique identifier for this spotlight zone
 * @param controller Controller managing all spotlight zones
 * @param modifier Modifier to be applied to the spotlight zone
 * @param config Configuration lambda applied to SpotlightZoneConfig
 * @param content Composable content to be highlighted by the spotlight
 *
 * Example:
 * ```kotlin
 * SpotlightZone(
 *     key = "feature_tour",
 *     controller = controller,
 *     config = {
 *         message {
 *             content { Text("Welcome to the new feature!") }
 *             audioResource(context, R.raw.intro)
 *             delay(5000)
 *         }
 *         message {
 *             content { Text("Tap to continue") }
 *             disableAudio()
 *         }
 *         tooltipPosition = TooltipPosition.BOTTOM
 *         shape = RoundedCornerShape(16.dp)
 *         onFinish = { analytics.trackTourComplete() }
 *     }
 * ) {
 *     FeatureButton()
 * }
 * ```
 */
@Composable
fun SpotlightZone(
    key: String,
    controller: SpotlightController,
    modifier: Modifier = Modifier,
    config: SpotlightZoneConfig.() -> Unit,
    content: @Composable () -> Unit
) {
    val configuration = remember(key) { SpotlightZoneConfig().apply(config) }

    SpotlightZone(
        key = key,
        controller = controller,
        modifier = modifier,
        messages = configuration.messages,
        tooltipPosition = configuration.tooltipPosition,
        tooltipAlignment = configuration.tooltipAlignment,
        disableTouch = configuration.disableTouch,
        forcedNavigation = configuration.forcedNavigation,
        adaptComponentShape = configuration.adaptComponentShape,
        shape = configuration.shape,
        onFinish = configuration.onFinish,
        content = content
    )
}

enum class TooltipPosition { TOP, BOTTOM, AUTO }

enum class TooltipAlignment { START, CENTER, END, AUTO }
